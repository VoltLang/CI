package org.volt_lang


class RepoConf implements Serializable
{
	/// Name for this repo, used for folder.
	def String name

	/// Git repo url.
	def String url

	/// Is this a library.
	def boolean lib

	// Is the 'repo' from the toolchain.
	def boolean toolchain

	/// Tag used in stash name
	def String tag

	/// Should a specific tag be used.
	def String gitTag
}
