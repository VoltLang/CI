package org.volt_lang


class RepoConf implements Serializable
{
	def tag
	def folder
	def url
	def lib


	RepoConf(folder = null, url = null, lib = false)
	{
		this.folder = folder
		this.url = url
		this.lib = lib
	}
}
