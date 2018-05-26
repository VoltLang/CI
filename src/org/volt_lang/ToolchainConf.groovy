package org.volt_lang


class ToolchainConf implements Serializable
{
	def wattTag
	def voltaTag
	def batteryTag

	ToolchainConf(Map config)
	{
		this.wattTag = "${config.watt}"
		this.voltaTag = "${config.volta}"
		this.batteryTag = "${config.battery}"
	}
}
