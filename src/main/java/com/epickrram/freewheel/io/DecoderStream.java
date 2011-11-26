//////////////////////////////////////////////////////////////////////////////////
//   Copyright 2011   Mark Price     mark at epickrram.com                      //
//                                                                              //
//   Licensed under the Apache License, Version 2.0 (the "License");            //
//   you may not use this file except in compliance with the License.           //
//   You may obtain a copy of the License at                                    //
//                                                                              //
//       http://www.apache.org/licenses/LICENSE-2.0                             //
//                                                                              //
//   Unless required by applicable law or agreed to in writing, software        //
//   distributed under the License is distributed on an "AS IS" BASIS,          //
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   //
//   See the License for the specific language governing permissions and        //
//   limitations under the License.                                             //
//////////////////////////////////////////////////////////////////////////////////
package com.epickrram.freewheel.io;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface DecoderStream
{
    public boolean readBoolean() throws IOException;
    public byte readByte() throws IOException;
    public int readInt() throws IOException;
    public long readLong() throws IOException;
    public float readFloat() throws IOException;
    public double readDouble() throws IOException;
    public byte[] readByteArray() throws IOException;
    public String readString() throws IOException;
    public <T> T readObject() throws IOException;
    public <T> void readCollection(final Collection<T> collection) throws IOException;
    public <K, V> void readMap(final Map<K, V> map) throws IOException;
}
