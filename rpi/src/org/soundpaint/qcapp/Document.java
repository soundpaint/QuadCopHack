/*
 * @(#)Document.java 1.00 17/01/24
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

public class Document
{
  public static interface Listener extends EventListener
  {
    public void documentChanged();
  }

  private final List<Listener> listeners;
  private final List<QuadCop.DataRecord> data;

  private Document()
  {
    listeners = new ArrayList<Listener> ();
    data = new ArrayList<QuadCop.DataRecord>();
  }

  public int size()
  {
    return data.size();
  }

  public boolean addListener(final Listener listener)
  {
    return listeners.add(listener);
  }

  public boolean removeListener(final Listener listener)
  {
    return listeners.remove(listener);
  }

  public int getNumberOfRecords()
  {
    return data.size();
  }

  public long getByteLength()
  {
    return getNumberOfRecords() * QuadCop.DataRecord.getByteLength();
  }

  public void saveToFile(final File file) throws IOException
  {
    final OutputStream out =
      new BufferedOutputStream(new FileOutputStream(file));
    for (final QuadCop.DataRecord record : data) {
      saveRecord(record, out);
    }
    out.close();
  }

  private void saveRecord(final QuadCop.DataRecord record,
                          final OutputStream out)
    throws IOException
  {
    out.write(record.getStatus());
    out.write(record.getCtrlLever0());
    out.write(record.getCtrlLever1());
    out.write(record.getCtrlLever2());
    out.write(record.getCtrlLever3());
    out.write(record.getButtons());
  }

  public static Document createNew()
  {
    return new Document();
  }

  public static Document createFromFile(final File file) throws IOException
  {
    final InputStream in =
      new BufferedInputStream(new FileInputStream(file));
    final Document document = new Document();
    while (in.available() > 0) {
      final QuadCop.DataRecord record = readRecord(in);
      document.data.add(record);
    }
    in.close();
    return document;
  }

  private static QuadCop.DataRecord readRecord(final InputStream in)
    throws IOException
  {
    final byte status = (byte)in.read();
    final byte ctrlLever0 = (byte)in.read();
    final byte ctrlLever1 = (byte)in.read();
    final byte ctrlLever2 = (byte)in.read();
    final byte ctrlLever3 = (byte)in.read();
    final byte buttons = (byte)in.read();
    final QuadCop.DataRecord record =
      new QuadCop.DataRecord(status,
                             ctrlLever0,
                             ctrlLever1,
                             ctrlLever2,
                             ctrlLever3,
                             buttons);
    return record;
  }

  public void clear()
  {
    data.clear();
  }

  private void notifyListeners()
  {
    for (Listener listener : listeners) {
      listener.documentChanged();
    }
  }

  public void addRecord(QuadCop.DataRecord record)
  {
    data.add(record);
    notifyListeners();
  }

  public int copyTo(final QuadCop.DataRecord[] destination,
                    final int startIndex, final int endIndex)
  {
    if (startIndex < 0) {
      throw new IllegalArgumentException("startIndex < 0");
    }
    if (endIndex > data.size()) {
      throw new IllegalArgumentException("endIndex > data.length");
    }
    if (startIndex > endIndex) {
      throw new IllegalArgumentException("startIndex > endIndex");
    }
    final int length = endIndex - startIndex;
    if (length > 0) {
      int destinationIndex = 0;
      for (int i = startIndex; i < endIndex; i++) {
        destination[destinationIndex++] = data.get(i);
      }
    }
    return length;
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
