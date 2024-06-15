package com.opentext.test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TaskExecutorImpl implements TaskExecutor {
	private final ExecutorService executorService;
	private final Map<UUID, ReentrantLock> taskGroupLocks;

	public TaskExecutorImpl(int maxConcurrency) {
		this.executorService = new ThreadPoolExecutor(maxConcurrency, maxConcurrency, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(), new RejectedExecutionHandlerImpl());
		this.taskGroupLocks = new ConcurrentHashMap<>();
	}

	@Override
	public <T> Future<T> submitTask(Task<T> task) {
		UUID taskGroupUUID = task.taskGroup().groupUUID();
		ReentrantLock lock = taskGroupLocks.computeIfAbsent(taskGroupUUID, k -> new ReentrantLock());
		lock.lock();
		try {
			return (Future<T>) executorService.submit(new TaskRunner<>(task, lock));
		} finally {
			lock.unlock();
		}
	}
}