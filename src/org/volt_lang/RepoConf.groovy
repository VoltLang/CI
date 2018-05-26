package org.volt_lang


class RepoConf implements Serializable
{
	def tag
	def name
	def url
	def lib
	// Is the repo from the toolchain.
	def toolchain

	/// Should a specific tag be used.
	def gitTag

	RepoConf(String name = null, String url = null, boolean lib = false,
	         String gitTag = null, boolean toolchain = false)
	{
		this.name = name
		this.url = url
		this.lib = lib
		this.toolchain = toolchain
		this.gitTag = gitTag
	}
}
