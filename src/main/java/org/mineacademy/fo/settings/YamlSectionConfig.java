package org.mineacademy.fo.settings;

/**
 * @deprecated removal pending, simply extend {@link YamlConfig} and use setPathPrefix(String) in its constructor
 */
@Deprecated
public class YamlSectionConfig extends YamlConfig {

	public YamlSectionConfig(String pathPrefix) {
		this.setPathPrefix(pathPrefix);
	}
}
