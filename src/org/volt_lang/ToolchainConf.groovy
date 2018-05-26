package org.volt_lang


class ToolchainConf implements Serializable
{
	def tag
	def wattTag
	def voltaTag
	def batteryTag

	ToolchainConf(watt = null, volta = null, battery = null, tag = null)
	{
		this.tag = tag
		this.wattTag = watt
		this.voltaTag = volta
		this.batteryTag = battery
	}
}
