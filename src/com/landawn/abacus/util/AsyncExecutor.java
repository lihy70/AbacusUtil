/*
 * Copyright (c) 2015, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.FN;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Predicate;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class AsyncExecutor {
    private static final Logger logger = LoggerFactory.getLogger(AsyncExecutor.class);

    private static final int DEFAULT_CORE_POOL_SIZE = Math.max(8, IOUtil.CPU_CORES);
    private static final int DEFAULT_MAX_THREAD_POOL_SIZE = Math.max(16, IOUtil.CPU_CORES);

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR;
    static {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(64);
        executor.setKeepAliveTime(180, TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(true);
        executor.setRemoveOnCancelPolicy(true);
        SCHEDULED_EXECUTOR = MoreExecutors.getExitingScheduledExecutorService(executor);
    }

    private final int coreThreadPoolSize;
    private final int maxThreadPoolSize;
    private final long keepAliveTime;
    private final TimeUnit unit;

    private volatile Executor executor;

    public AsyncExecutor() {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, 300, TimeUnit.SECONDS);
    }

    public AsyncExecutor(int maxThreadPoolSize, long keepAliveTime, TimeUnit unit) {
        this(DEFAULT_CORE_POOL_SIZE, maxThreadPoolSize, keepAliveTime, unit);
    }

    public AsyncExecutor(int coreThreadPoolSize, int maxThreadPoolSize, long keepAliveTime, TimeUnit unit) {
        N.checkArgNotNegative(coreThreadPoolSize, "coreThreadPoolSize");
        N.checkArgNotNegative(maxThreadPoolSize, "maxThreadPoolSize");
        N.checkArgNotNegative(keepAliveTime, "keepAliveTime");
        N.checkArgNotNull(unit, "unit");

        this.coreThreadPoolSize = coreThreadPoolSize;
        this.maxThreadPoolSize = maxThreadPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (executor == null || !(executor instanceof ExecutorService)) {
                    return;
                }

                final ExecutorService executorService = (ExecutorService) executor;
                logger.warn("Starting to shutdown task in AsyncExecutor");

                try {
                    executorService.shutdown();

                    while (executorService.isTerminated() == false) {
                        N.sleepUninterruptibly(100);
                    }
                } finally {
                    logger.warn("Completed to shutdown task in AsyncExecutor");
                }
            }
        });
    }

    /**
     * 
     * @param asyncExecutor
     */
    public AsyncExecutor(final Executor executor) {
        this(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_THREAD_POOL_SIZE, 300, TimeUnit.SECONDS);

        this.executor = executor;
    }

    public ContinuableFuture<Void> execute(final Try.Runnable<? extends Exception> command) {
        return execute(new FutureTask<Void>(FN.toCallable(command)));
    }

    public ContinuableFuture<Void> execute(final Try.Runnable<? extends Exception> action, final long delay) {
        return execute(action, delay, TimeUnit.MILLISECONDS);
    }

    public ContinuableFuture<Void> execute(final Try.Runnable<? extends Exception> action, final long delay, final TimeUnit timeUnit) {
        final Executor executor = getExecutor();

        final Callable<ContinuableFuture<Void>> scheduledAction = new Callable<ContinuableFuture<Void>>() {
            @Override
            public ContinuableFuture<Void> call() throws Exception {
                return execute(action);
            }
        };

        final ScheduledFuture<ContinuableFuture<Void>> scheduledFuture = SCHEDULED_EXECUTOR.schedule(scheduledAction, delay, timeUnit);

        return new ContinuableFuture<>(wrap(scheduledFuture), null, executor);
    }

    @SafeVarargs
    public final List<ContinuableFuture<Void>> execute(final Try.Runnable<? extends Exception>... commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.length);

        for (int i = 0, len = commands.length; i < len; i++) {
            results.add(execute(commands[i]));
        }

        return results;
    }

    public List<ContinuableFuture<Void>> execute(final List<? extends Try.Runnable<? extends Exception>> commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<Void>> results = new ArrayList<>(commands.size());

        for (Try.Runnable<? extends Exception> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    public <T> ContinuableFuture<T> execute(final Callable<T> command) {
        return execute(new FutureTask<>(command));
    }

    public <T> ContinuableFuture<T> execute(final Callable<T> action, final long delay) {
        return execute(action, delay, TimeUnit.MILLISECONDS);
    }

    public <T> ContinuableFuture<T> execute(final Callable<T> action, final long delay, final TimeUnit timeUnit) {
        final Executor executor = getExecutor();

        final Callable<ContinuableFuture<T>> scheduledAction = new Callable<ContinuableFuture<T>>() {
            @Override
            public ContinuableFuture<T> call() throws Exception {
                return execute(action);
            }
        };

        final ScheduledFuture<ContinuableFuture<T>> scheduledFuture = SCHEDULED_EXECUTOR.schedule(scheduledAction, delay, timeUnit);

        return new ContinuableFuture<>(wrap(scheduledFuture), null, executor);
    }

    @SafeVarargs
    public final <T> List<ContinuableFuture<T>> execute(final Callable<T>... commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<T>> results = new ArrayList<>(commands.length);

        for (int i = 0, len = commands.length; i < len; i++) {
            results.add(execute(commands[i]));
        }

        return results;
    }

    public <T> List<ContinuableFuture<T>> execute(final Collection<? extends Callable<T>> commands) {
        if (N.isNullOrEmpty(commands)) {
            return new ArrayList<>();
        }

        final List<ContinuableFuture<T>> results = new ArrayList<>(commands.size());

        for (Callable<T> cmd : commands) {
            results.add(execute(cmd));
        }

        return results;
    }

    public ContinuableFuture<Void> execute(final Try.Runnable<? extends Exception> action, final int retryTimes, final long retryInterval,
            final Predicate<? super Exception> retryCondition) {
        return execute(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Retry.of(retryTimes, retryInterval, retryCondition).run(action);
                return null;
            }
        });
    }

    public <T> ContinuableFuture<T> execute(final Callable<T> action, final int retryTimes, final long retryInterval,
            final BiPredicate<? super T, ? super Exception> retryCondition) {
        return execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                final Retry<T> retry = Retry.of(retryTimes, retryInterval, retryCondition);
                return retry.call(action);
            }
        });
    }

    private <T> ContinuableFuture<T> execute(final FutureTask<T> futureTask) {
        final Executor executor = getExecutor();

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    private static <T> Future<T> wrap(final ScheduledFuture<ContinuableFuture<T>> scheduledFuture) {
        return new Future<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (scheduledFuture.cancel(mayInterruptIfRunning) && scheduledFuture.isDone()) {
                    try {
                        final ContinuableFuture<T> resFuture = scheduledFuture.get();
                        return resFuture == null || resFuture.cancel(mayInterruptIfRunning);
                    } catch (Exception e) {
                        return false;
                    }
                }

                return false;
            }

            @Override
            public boolean isCancelled() {
                if (scheduledFuture.isCancelled() && scheduledFuture.isDone()) {
                    try {
                        final ContinuableFuture<T> resFuture = scheduledFuture.get();
                        return resFuture == null || resFuture.isCancelled();
                    } catch (Exception e) {
                        return false;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                if (scheduledFuture.isDone()) {
                    try {
                        final ContinuableFuture<T> resFuture = scheduledFuture.get();
                        return resFuture == null || resFuture.isDone();
                    } catch (Exception e) {
                        return false;
                    }
                }

                return false;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                final ContinuableFuture<T> resFuture = scheduledFuture.get();
                return resFuture == null ? null : resFuture.get();
            }

            @Override
            public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final long beginTime = System.currentTimeMillis();

                final ContinuableFuture<T> resFuture = scheduledFuture.get(timeout, unit);

                final long remainingTimeout = unit.toMillis(timeout) - (System.currentTimeMillis() - beginTime);

                return resFuture == null ? null : (remainingTimeout > 0 ? resFuture.get(remainingTimeout, TimeUnit.MILLISECONDS) : resFuture.get());
            }
        };
    }

    private Executor getExecutor() {
        if (executor == null) {
            synchronized (this) {
                if (executor == null) {
                    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, unit,
                            new LinkedBlockingQueue<Runnable>());
                    threadPoolExecutor.allowCoreThreadTimeOut(true);
                    executor = threadPoolExecutor;
                }
            }
        }

        return executor;
    }
}
