package org.volt_lang


class NodeConf implements Serializable
{
	def tag
	def node
	def arch
	def plat
	def cross
	def dir


	NodeConf(arch, plat, cross, node)
	{
		this.tag = "${arch}-${plat}"
		this.arch = arch
		this.plat = plat
		this.node = node
		this.cross = cross
	}
}
