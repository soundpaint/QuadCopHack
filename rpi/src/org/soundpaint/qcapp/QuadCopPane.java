/*
 * @(#)QuadCopPane.java 1.00 17/01/24
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

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class QuadCopPane extends JComponent implements QCRecorder
{
  private static final long serialVersionUID = -7333199261388238367L;

  private final QuadCopApp quadCopApp;
  private boolean loaded;
  private BufferedImage quadCopImage;
  private BufferedImage leftCtrlImage;
  private BufferedImage rightCtrlImage;
  private byte status;
  private byte ctrlLever0;
  private byte ctrlLever1;
  private byte ctrlLever2;
  private byte ctrlLever3;
  private byte buttons;

  private QuadCopPane()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public QuadCopPane(final QuadCopApp quadCopApp)
  {
    if (quadCopApp == null) {
      throw new NullPointerException("quadCopApp");
    }
    this.quadCopApp = quadCopApp;
    loaded = false;
    Dimension dimension = new Dimension(200, 100);
    quadCopImage = null;
    leftCtrlImage = null;
    rightCtrlImage = null;
    try {
      quadCopImage = createImage("quadcop_small.png");
      leftCtrlImage = createImage("left-ctrl_small.png");
      rightCtrlImage = createImage("right-ctrl_small.png");
      dimension =
        new Dimension(quadCopImage.getWidth(), quadCopImage.getHeight());
      loaded = true;
    } catch (final IOException ex) {
      showLoadError(ex.getMessage());
    } catch (final IllegalArgumentException ex) {
      showLoadError(ex.getMessage());
    }
    setPreferredSize(dimension);
  }

  private BufferedImage createImage(final String imageFileName)
    throws IOException
  {
    final String imagePath = "/images/" + imageFileName;
    final URL imageURL = QuadCopApp.class.getResource(imagePath);
    if (imageURL != null) {
      return ImageIO.read(imageURL);
    } else {
      System.err.println("Resource not found: " + imagePath);
      throw new IOException("Resource not found: " + imagePath);
    }
  }

  private void showLoadError(String msg)
  {
    final String title = "failed loading background images";
    JOptionPane.showMessageDialog(quadCopApp, msg, title,
                                  JOptionPane.ERROR_MESSAGE);
  }

  protected void paintComponent(final Graphics g)
  {
    if (loaded) {
      final int leftCtrlX = (int)(95.0 + (ctrlLever1 & 0xff) * 0.125);
      final int leftCtrlY = (int)(95.0 - (ctrlLever0 & 0xff) * 0.125);
      final int rightCtrlX = (int)(358.0 - (ctrlLever3 & 0xff) * 0.125);
      final int rightCtrlY = (int)(67.0 + (ctrlLever2 & 0xff) * 0.125);

      g.drawImage(quadCopImage, 0, 0, this);
      g.drawImage(leftCtrlImage, leftCtrlX, leftCtrlY, this);
      g.drawImage(rightCtrlImage, rightCtrlX, rightCtrlY, this);
    } else {
      super.paintComponent(g);
    }
  }

  private byte count = 0; // DEBUG

  public void recordReceived(final QuadCop.DataRecord record)
  {
    // DEBUG START
    /*
    if (count == 0) {
      System.out.println(record);
    }
    count = (byte)((count + 1) & 0x7f);
    */
    // DEBUG END

    status = record.getStatus();
    ctrlLever0 = record.getCtrlLever0();
    ctrlLever1 = record.getCtrlLever1();
    ctrlLever2 = record.getCtrlLever2();
    ctrlLever3 = record.getCtrlLever3();
    buttons = record.getButtons();
    repaint();
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
