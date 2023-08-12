package org.mineacademy.fo.scheduler;

import java.lang.reflect.Method;

import org.mineacademy.fo.ReflectionUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FoliaTask implements Task {

	private final Method cancelMethod;
	private final Object taskInstance;

	@Override
	public void cancel() {
		ReflectionUtil.invoke(this.cancelMethod, this.taskInstance);
	}
}