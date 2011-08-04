#!/usr/bin/env python
#
# Copyright (c) 2010-2011 Appcelerator, Inc. All Rights Reserved.
# Licensed under the Apache Public License (version 2)

import sys, os
from common import info, err, warn, msg

try:
	import json
except:
	import simplejson as json

def generate(raw_apis, annotated_apis, options):
	warn("JSCA is not yet supported. A skeleton api.jsca will be created.")
	out_path = os.path.join(options.output, "api.jsca")
	api = {"types": [], "aliases": [{"type": "Titanium", "name": "Ti"}]}
	f = open(out_path, "w")
	json.dump(api, f, indent=4)
	print json.dumps(api, indent=4)
	f.close()
