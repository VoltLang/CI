package org.volt_lang


class NodeConf implements Serializable
{
	/// Node name as used by Jenkins.
	def String node

	/// Arch that this node builds on.
	def String arch

	/// Platform that this node builds on.
	def String plat

	/// Is this config cross compiling.
	def boolean cross

	/// Filled by Builder.
	def String dir
}
