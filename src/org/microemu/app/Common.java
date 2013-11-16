/**
 *  MicroEmulator
 *  Copyright (C) 2001-2003 Bartek Teodorczyk <barteo@barteo.net>
 *
 *  It is licensed under the following two licenses as alternatives:
 *    1. GNU Lesser General Public License (the "LGPL") version 2.1 or any newer version
 *    2. Apache License (the "AL") Version 2.0
 *
 *  You may not use this file except in compliance with at least one of
 *  the above two licenses.
 *
 *  You may obtain a copy of the LGPL at
 *      http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
 *
 *  You may obtain a copy of the AL at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the LGPL or the AL for the specific language governing permissions and
 *  limitations.
 *
 *  @version $Id: Common.java 2517 2011-11-10 12:30:37Z barteo@gmail.com $
 */
package org.microemu.app;

import org.microemu.MIDletBridge;
import org.microemu.MicroEmulator;
import org.microemu.RecordStoreManager;
import org.microemu.microedition.ImplFactory;
import org.microemu.microedition.io.ConnectorImpl;

public class Common implements MicroEmulator {

    private RecordStoreManager recordStoreManager;

    public Common() {
        /*
         * Initialize secutity context for implemenations, May be there are better place
         * for this call
         */
        ImplFactory.instance();
        // TODO integrate with ImplementationInitialization
        ImplFactory.registerGCF(ImplFactory.DEFAULT, new ConnectorImpl());
    }

    public RecordStoreManager getRecordStoreManager() {
        return recordStoreManager;
    }

    public void setRecordStoreManager(RecordStoreManager manager) {
        this.recordStoreManager = manager;
    }

    public void initMIDlet() {
        MIDletBridge.getRecordStoreManager().init(MIDletBridge.getMicroEmulator());
    }
}
