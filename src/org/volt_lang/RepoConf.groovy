package org.volt_lang


class RepoConf implements Serializable
{
	/// Name for this repo, used for folder.
	def String name

	/// Is this a library.
	def boolean lib

	// Is the 'repo' from the toolchain.
	def boolean toolchain

	/// Git repo url.
	def String gitUrl

	/// Should a specific tag be used.
	def String gitTag

	/// Tag used in stash name
	def String stashName
}
