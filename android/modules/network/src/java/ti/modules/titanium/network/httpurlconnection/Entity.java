/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
//org.apache.http.entity.AbstractHttpEntity
/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network.httpurlconnection;

import java.io.IOException;
import java.io.OutputStream;

public abstract class Entity {
    protected String contentType;
    protected String contentEncoding;
    
    public Entity() {
    	
    }
    
    public String getContentType() {
        return this.contentType;
    }
    
    public String getContentEncoding() {
        return this.contentEncoding;
    }
    
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }
    
    public void setContentEncoding(final String contentEncoding) {
        this.contentEncoding = contentEncoding;
    }
    
    public abstract void writeTo(OutputStream out) throws IOException;


}
