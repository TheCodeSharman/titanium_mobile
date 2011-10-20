#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# Project Compiler
#

import os, sys, re, shutil, time, base64, sgmllib, codecs, xml, json

template_dir = os.path.abspath(os.path.dirname(sys._getframe(0).f_code.co_filename))
from tiapp import *
import jspacker 

ignoreFiles = ['.gitignore', '.cvsignore', '.DS_Store'];
ignoreDirs = ['.git','.svn','_svn','CVS','android','iphone'];

HEADER = """/**
 * Appcelerator Titanium Mobile Web SDK - http://appcelerator.com
 * This is generated code. Do not modify. Your changes *will* be lost.
 * Generated code is Copyright (c) 2011 by Appcelerator, Inc.
 * All Rights Reserved.
 */
"""

FOOTER = """"""

class Compiler(object):
	def __init__(self,project_dir,deploytype):
		self.project_dir = project_dir
		self.modules = []
		self.defines = ['screen.js', 'interactable.js', 'clickable.js', 'eventdriven.js', 'styleable.js', 'touchable.js', 'positionable.js', 'domview.js', 'Ti/ti.js', 'Ti.App/properties.js', 'Ti.Locale/locale.js', 'titanium.css']
		self.css_defines = []
		self.ti_includes = {}
		self.api_map = {}
		
		self.build_dir = os.path.join(self.project_dir,'build','mobileweb')
		
		self.resources_dir = os.path.join(self.project_dir,'Resources')
		self.debug = False
		self.count = 0
		
		if deploytype == 'development' or deploytype == 'all':
			self.debug = True

		src_dir = os.path.join(template_dir,'src')

		if os.path.exists(self.build_dir):
			shutil.rmtree(self.build_dir, True)

		try:
			os.makedirs(self.build_dir)
		except:
			pass
			
		# load up our API map
		map_props = open(os.path.join(src_dir,'map.prop')).read()
		for line in map_props.split("\n"):
			if line[0:1] == '#' or line[0:1]=='': continue
			key,value = line.split("=")
			self.api_map[key.strip()]=value.strip().split()

		tiapp_xml = os.path.join(project_dir,'tiapp.xml')
		ti = TiAppXML(tiapp_xml)
		sdk_version = os.path.basename(os.path.abspath(os.path.join(template_dir,'../')))

		self.project_name = ti.properties['name']
		self.appid = ti.properties['id']

		if ti.properties['analytics']:
			self.defines.append("Ti.Platform/platform.js")

		def compile_js(from_,to_):
			js = Compiler.make_function_from_file(from_,self)
			o = codecs.open(to_,'w',encoding='utf-8')
			o.write(js)
			o.close()
			self.count+=1

		source = self.resources_dir
		target = self.build_dir

		for root, dirs, files in os.walk(source):
			for name in ignoreDirs:
				if name in dirs:
					dirs.remove(name)	# don't visit ignored directories
			for file in files:
				if file in ignoreFiles:
					continue
				from_ = os.path.join(root, file)
				to_ = os.path.expanduser(from_.replace(source, target, 1))
				to_directory = os.path.expanduser(os.path.split(to_)[0])
				if not os.path.exists(to_directory):
					try:
						os.makedirs(to_directory)
					except:
						pass
				fp = os.path.splitext(file)
				if fp[1]=='.js':
					compile_js(from_,to_)
				else:
					shutil.copy(from_,to_)

		titanium_js = "(%s\n\
)(window, {\n\
	projectName: '%s',\n\
	projectId: '%s',\n\
	deployType: '%s',\n\
	appId: '%s',\n\
	appAnalytics: '%s',\n\
	appPublisher: '%s',\n\
	appUrl: '%s',\n\
	appName: '%s',\n\
	appVersion: '%s',\n\
	appDescription: '%s',\n\
	appCopyright: '%s',\n\
	appGuid: '%s',\n\
	tiVersion: '%s'\n\
});\n".encode('utf-8') % (
			self.load_api(os.path.join(src_dir,"titanium.js")),
			self.project_name,
			self.appid,
			deploytype,
			self.appid,
			ti.properties['analytics'],
			ti.properties['publisher'],
			ti.properties['url'],
			ti.properties['name'],
			ti.properties['version'],
			ti.properties['description'],
			ti.properties['copyright'],
			ti.properties['guid'],
			sdk_version
		)

		if deploytype == 'all':
			print "Deploy type is 'all' - all modules will be included into dist"
			for root, dirs, files in os.walk(src_dir):
				for name in ignoreDirs:
					if name in dirs:
						dirs.remove(name)	# don't visit ignored directories
				for file in files:
					if file in ignoreFiles or file == 'titanium.js':
						continue
					path = os.path.join(root, file)
					fp = os.path.splitext(file)
					if fp[1]=='.js':
						(path, fname) = os.path.split(path)
						(path, ddir) = os.path.split(path)
						if ddir != 'src':
							fname = ddir + "/" + fname

						try:
							self.defines.index(fname)
						except:
							self.defines.append(fname)

		titanium_css = ''
		for api in self.defines:
			api_file = os.path.join(src_dir,api)
			if not os.path.exists(api_file):
				print "[ERROR] couldn't find file: %s" % api_file
				sys.exit(1)
			else:
				print "[DEBUG] found: %s" % api_file
				if api_file.find('.js') != -1:
					titanium_js+='%s;\n' % self.load_api(api_file, api)
				elif api_file.find('.css') != -1:
					titanium_css +='%s\n\n' % self.load_api(api_file, api)
				else:
					target_file = os.path.abspath(os.path.join(self.build_dir,'titanium', api))
					try:
						os.makedirs(os.path.dirname(target_file))
					except:
						pass

					open(target_file,'wb').write(open(api_file,'rb').read())
		titanium_js += ";\nTi._5.setLoadedScripts(" + json.dumps(self.ti_includes) + ");"

		titanium_js = HEADER + titanium_js + FOOTER

		titanium_js = titanium_js.replace('__PROJECT_NAME__',self.project_name)
		titanium_js = titanium_js.replace('__PROJECT_ID__',self.appid)
		titanium_js = titanium_js.replace('__DEPLOYTYPE__',deploytype)
		titanium_js = titanium_js.replace('__APP_ID__',self.appid)
		titanium_js = titanium_js.replace('__APP_ANALYTICS__',ti.properties['analytics'])
		titanium_js = titanium_js.replace('__APP_PUBLISHER__',ti.properties['publisher'])
		titanium_js = titanium_js.replace('__APP_URL__',ti.properties['url'])
		titanium_js = titanium_js.replace('__APP_NAME__',ti.properties['name'])
		titanium_js = titanium_js.replace('__APP_VERSION__',ti.properties['version'])
		titanium_js = titanium_js.replace('__APP_DESCRIPTION__',ti.properties['description'])
		titanium_js = titanium_js.replace('__APP_COPYRIGHT__',ti.properties['copyright'])
		titanium_js = titanium_js.replace('__APP_GUID__',ti.properties['guid'])
		

		ti_dir = os.path.join(self.build_dir,'titanium')
		try:
			os.makedirs(ti_dir)
		except:
			pass
		
		o = codecs.open(os.path.join(ti_dir,'titanium.js'),'w',encoding='utf-8')
		o.write(titanium_js)
		o.close()

		titanium_css = HEADER + titanium_css + FOOTER

		o = codecs.open(os.path.join(ti_dir,'titanium.css'), 'w', encoding='utf-8')
		o.write(titanium_css)
		o.close()

		try:
			status_bar_style = ti.properties['statusbar-style']
			
			if status_bar_style == 'default' or status_bar_style=='grey':
				status_bar_style = 'default'
			elif status_bar_style == 'opaque_black' or status_bar_style == 'opaque' or status_bar_style == 'black':
				status_bar_style = 'black'
			elif status_bar_style == 'translucent_black' or status_bar_style == 'transparent' or status_bar_style == 'translucent':
				status_bar_style = 'black-translucent'
			else:	
				status_bar_style = 'default'
		except:
			status_bar_style = 'default'

		main_template = codecs.open(os.path.join(src_dir,'index.html'), encoding='utf-8').read().encode("utf-8")
		main_template = main_template.replace('__TI_VERSION__',sdk_version)
		main_template = main_template.replace('__TI_STATUSBAR_STYLE__',status_bar_style)
		main_template = main_template.replace('__TI_GENERATOR__',"Appcelerator Titanium Mobile "+sdk_version)
		main_template = main_template.replace('__PROJECT_NAME__',self.project_name)
		main_template = main_template.replace('__PROJECT_ID__',self.appid)
		main_template = main_template.replace('__DEPLOYTYPE__',deploytype)
		main_template = main_template.replace('__APP_ID__',self.appid)
		main_template = main_template.replace('__APP_ANALYTICS__',ti.properties['analytics'])
		main_template = main_template.replace('__APP_PUBLISHER__',ti.properties['publisher'])
		main_template = main_template.replace('__APP_URL__',ti.properties['url'])
		main_template = main_template.replace('__APP_NAME__',ti.properties['name'])
		main_template = main_template.replace('__APP_VERSION__',ti.properties['version'])
		main_template = main_template.replace('__APP_DESCRIPTION__',ti.properties['description'])
		main_template = main_template.replace('__APP_COPYRIGHT__',ti.properties['copyright'])
		main_template = main_template.replace('__APP_GUID__',ti.properties['guid'])
		main_template = main_template.replace('__TI_JS__',titanium_js)

		index_file = os.path.join(self.build_dir,'index.html')
		o = codecs.open(index_file,'w',encoding='utf-8')
		o.write(main_template)
		o.close()

		# write localization data
		i18n_content = "Titanium._5.setLocaleData("
		def xml2json(collector, node):
			collector[node.attributes.items()[0][1]] = node.firstChild.nodeValue
			return collector

		lang_arr = {}
		for root, dirs, files in os.walk(os.path.join(self.project_dir,'i18n')):
			for file in files:
				if file != 'strings.xml':
					continue
				lang = os.path.split(root)[1]
				lang_arr[lang] = {}
				lang_file = codecs.open(os.path.join(root, file), 'r', 'utf-8').read().encode("utf-8")
				dom = xml.dom.minidom.parseString(lang_file)
				strings = dom.getElementsByTagName("string")
				reduce(xml2json, strings, lang_arr[lang])
		i18n_content += json.dumps(lang_arr)

		i18n_content += ");";
		i18n_file = os.path.join(self.build_dir,'titanium', 'i18n.js')
		o = codecs.open(i18n_file,'w', encoding='utf-8')
		o.write(i18n_content)
		o.close()
		print "[INFO] Compiled %d files for %s" % (self.count,ti.properties['name'])
		
		
	def load_api(self,file, api=""):
		file_contents = codecs.open(file, 'r', 'utf-8').read()
		if not self.debug and file.find('.js') != -1:
			return jspacker.jsmin(file_contents)
		elif file.find('.css') != -1:
			# need to replace urls to add directory prefix into path
			return re.sub(r'(url\s*\([\'"]?)', r'\1' + os.path.split(api)[0] + '/', file_contents)
		else:
			return file_contents
		
	def add_symbol(self,api):
#		print "[DEBUG] detected symbol: %s" % api
		curtoken = ''
		tokens = api.split(".")
		if len(tokens) > 1:
			try:
				self.modules.index(tokens[0])
			except:
				self.modules.append(tokens[0])
			
		if self.api_map.has_key(api):
			for file in self.api_map[api]:
				if len(tokens) > 1:
					fn = "Ti.%s/%s" % (tokens[0],file)
				else:
					fn = "Ti/%s" % file
				try:
					self.defines.index(fn)
				except:
					self.defines.append(fn)
		else:
			print "[WARN] couldn't find API: %s" % api
			#sys.exit(1)

	def extract_tokens(self,sym,line):
		# sloppy joe parsing coooode
		# could be prettier and faster but it works and rather reliable
		c = 0
		tokens = []
		search = sym + "."
		size = len(search)
		while True:
			i = line.find(search,c)
			if i < 0:
				break
			found = False
			buf = ''
			x = 0
			for n in line[i+size:]:
				# look for a terminal - this could probably be easier
				if n in ['(',')','{','}','=',',',' ',':','!','[',']','+','*','/','~','^','%','\n','\t','\r']:
					found = True
					break
				buf+=n
				x+=1
			tokens.append(buf)
			if found:
				c = i + x + 1
				continue
			break
		return tokens	

	def expand_ti_includes(self,line,filename):
		idx = line.find('Ti.include')
		if idx!=-1:
			srcs = line[idx+11:-1]
			for srcQ in srcs.split(','):
				# remove leading and trailing slashes and spaces
				src = re.sub(r'\s*([\"\'])([^\1]*)\1[\w\W]*$', r'\2', srcQ, 0, re.M)

				# replace dir separator with platform specific
				# if first char is / - consider it as absolute to resources dir
				if src[0] == '/':
					src_path = os.path.join(self.resources_dir,src[1:len(src)])
				else:
					src_path = os.path.join(os.path.dirname(filename),src)
				# normalize path to match all dir separators
				src_path = os.path.normpath(src_path)

				if not os.path.exists(src_path):
					print "[ERROR] Cannot find include file at: %s" % src_path
					sys.exit(1)
				source = Compiler.make_function_from_file(src_path,self)
				self.ti_includes[src] = source

	def compile_js(self,file_contents,fn):
		contents = ""
		for line in file_contents.split(';'):
			self.expand_ti_includes(line,fn)
			if line == None or line=='' or line == '\n': continue
			for sym in self.extract_tokens('Ti',line):
				self.add_symbol(sym)
			contents+='%s;' % line
		return contents
	
	@classmethod
	def make_function_from_file(cls,file,instance=None):
		f = os.path.expanduser(file)
		file_contents = codecs.open(f, 'r', 'utf-8').read()
		if not instance or not instance.debug:
			file_contents = jspacker.jsmin(file_contents)
		file_contents = file_contents.replace('Titanium.','Ti.')
		if instance:
			file_contents = instance.compile_js(file_contents, f)
		return file_contents
