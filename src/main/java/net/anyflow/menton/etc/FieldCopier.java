/**
 * 
 */
package net.anyflow.menton.etc;

import java.lang.reflect.Field;

/**
 * @author anyflow
 */
public class FieldCopier {

	public static void copy(Object source, Object target) throws IllegalArgumentException, IllegalAccessException {
		Field[] sourceFields = source.getClass().getDeclaredFields();
		Field[] targetFields = target.getClass().getDeclaredFields();

		for(Field sf : sourceFields) {
			if(sf.isSynthetic()) {
				continue;
			}

			for(Field tf : targetFields) {
				if(tf.getName().equals(sf.getName()) == false) {
					continue;
				}

				if(tf.getType().equals(sf.getType()) == false) {
					continue;
				}

				if(sf.isAccessible() == false) {
					sf.setAccessible(true);
					tf.set(target, sf.get(source));
					sf.setAccessible(false);
				}
				else {
					tf.set(target, sf.get(source));
				}
			}
		}
	}
}
