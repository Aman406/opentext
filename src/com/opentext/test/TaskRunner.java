package com.opentext.test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

class TaskRunner<T> implements Runnable, Future<T> {
	private final Task<T> task;
	private final ReentrantLock lock;
	private T result;
	private Throwable exception;
	private boolean done;

	public TaskRunner(Task<T> task, ReentrantLock lock) {
		this.task = task;
		this.lock = lock;
	}

	@Override
	public void run() {
		try {
			result = task.taskAction().call();
		} catch (Throwable e) {
			exception = e;
		} finally {
			done = true;
			lock.unlock();
			signalWaiters();
		}
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		await();
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return result;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (await(timeout, unit)) {
			if (exception != null) {
				throw new ExecutionException(exception);
			}
			return result;
		} else {
			throw new TimeoutException();
		}
	}

	private void await() throws InterruptedException {
		lock.lock();
		try {
			while (!done) {
				lock.wait();
			}
		} finally {
			lock.unlock();
		}
	}

	private boolean await(long timeout, TimeUnit unit) throws InterruptedException {
	    lock.lock();
	    try {
	        long startTime = System.nanoTime();
	        long nanos = unit.toNanos(timeout);
	        while (!done) {
	            if (nanos <= 0) {
	                return false;
	            }
	            lock.wait(nanos / 1000000, (int) (nanos % 1000000));
	            nanos -= System.nanoTime() - startTime;
	            startTime = System.nanoTime();
	        }
	        return true;
	    } finally {
	        lock.unlock();
	    }
	}

	private void signalWaiters() {
		lock.lock();
		try {
			lock.notifyAll();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isCancelled() {
		return false;
	}
}