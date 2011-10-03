/*
Copyright 2011 Mark Price

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.epickrram.freewheel.io;

import java.io.IOException;

public interface EncoderStream
{
    public void writeBoolean(final boolean v) throws IOException;
    public void writeByte(final byte v) throws IOException;
    public void writeInt(final int v) throws IOException;
    public void writeLong(final long v) throws IOException;
    public void writeFloat(final float v) throws IOException;
    public void writeDouble(final double v) throws IOException;
    public void writeByteArray(final byte[] b) throws IOException;
    public void writeByteArray(final byte[] b, int off, int len) throws IOException;
    public void writeString(final String s) throws IOException;
    public <T> void writeObject(final T o) throws IOException;
}
