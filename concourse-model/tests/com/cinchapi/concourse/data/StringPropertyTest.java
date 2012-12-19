package com.cinchapi.concourse.data;

import com.cinchapi.commons.util.RandomString;
import com.cinchapi.concourse.data.Property;
import com.cinchapi.concourse.data.StringProperty;

/**
 * Test for {@link StringProperty}
 * @author jnelson
 *
 */
public class StringPropertyTest extends PropertyTest<String>{

	@Override
	public Property<String> getInstance(String key, String value) {
		return new StringProperty(key, value);
	}

	@Override
	public String getRandomValue() {
		RandomString random = new RandomString();
		return random.nextStringAllowDigits();
	}

}