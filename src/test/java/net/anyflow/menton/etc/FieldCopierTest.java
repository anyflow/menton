/**
 * 
 */
package net.anyflow.menton.etc;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.junit.Test;

/**
 * @author anyflow
 */
public class FieldCopierTest {

	class Source {

		public Source(String field1, int field2, double field3, String sourceSpecific) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.sourceSpecific = sourceSpecific;
		}

		String field1;

		/**
		 * @return the field1
		 */
		public String getField1() {
			return field1;
		}

		/**
		 * @return the field2
		 */
		public int getField2() {
			return field2;
		}

		/**
		 * @return the field3
		 */
		public double getField3() {
			return field3;
		}

		/**
		 * @return the sourceSpecific
		 */
		public String getSourceSpecific() {
			return sourceSpecific;
		}

		int field2;
		double field3;

		String sourceSpecific;

		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 31).append(field1).append(field2).append(field3).append(sourceSpecific)
					.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
	}

	class Target {

		public Target(String field1, int field2, double field3, String targetSpecific) {
			this.field1 = field1;
			this.field2 = field2;
			this.field3 = field3;
			this.targetSpecific = targetSpecific;
		}

		String field1;

		/**
		 * @return the field1
		 */
		public String getField1() {
			return field1;
		}

		/**
		 * @return the field2
		 */
		public int getField2() {
			return field2;
		}

		/**
		 * @return the field3
		 */
		public double getField3() {
			return field3;
		}

		/**
		 * @return the targetSpecific
		 */
		public String getTargetSpecific() {
			return targetSpecific;
		}

		int field2;
		double field3;

		String targetSpecific;

		@Override
		public int hashCode() {
			return new HashCodeBuilder(17, 31).append(field1).append(field2).append(field3).append(targetSpecific)
					.toHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
	}

	@Test
	public void copyTest() throws IllegalArgumentException, IllegalAccessException {

		String targetSpecific = "target!!";
		Source source = new Source("copy?", 1, 1.1, "source!!");
		Target target = new Target("", 3, 4.1, targetSpecific);

		FieldCopier.copy(source, target);

		assertThat(source.getField1(), is(target.getField1()));
		assertThat(source.getField2(), is(target.getField2()));
		assertThat(source.getField3(), is(target.getField3()));
		assertThat(target.getTargetSpecific(), is(targetSpecific));
	}
}
