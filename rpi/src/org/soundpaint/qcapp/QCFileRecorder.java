/*
 * @(#)QCFileRecorder.java 1.00 17/01/26
 *
 * Copyright (C) 2017 Jürgen Reuter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.soundpaint.qcapp;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class QCFileRecorder implements QCRecorder
{
  private final String outFileName;
  private final OutputStream out;
  private final byte[] outBuffer;
  private IOException starvationException;

  private QCFileRecorder()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public QCFileRecorder(final String outFileName) throws IOException
  {
    this.outFileName = outFileName;
    out = new BufferedOutputStream(new FileOutputStream(outFileName));
    outBuffer = new byte[6];
    starvationException = null;
  }

  public String getOutFileName()
  {
    return outFileName;
  }

  public IOException getStarvationException()
  {
    return starvationException;
  }

  public void recordReceived(final QuadCop.DataRecord record)
  {
    if (starvationException == null) {
      outBuffer[0] = record.getStatus();
      outBuffer[1] = record.getCtrlLever0();
      outBuffer[2] = record.getCtrlLever1();
      outBuffer[3] = record.getCtrlLever2();
      outBuffer[4] = record.getCtrlLever3();
      outBuffer[5] = record.getButtons();
      try {
        out.write(outBuffer);
      } catch (final IOException ex) {
        starvationException = ex;
      }
    }
  }

  public void close() throws IOException
  {
    out.close();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
