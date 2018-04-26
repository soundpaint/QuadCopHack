/*
 * @(#)MenuBar.java 1.00 17/01/27
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

public class MenuBar extends JMenuBar
  implements TransportControl.Listener, DocumentManager.Listener
{
  private static final long serialVersionUID = -2217863353294640984L;

  final private QuadCopApp quadCopApp;
  final private TransportControl transportControl;
  final private DocumentManager documentManager;
  private JMenuItem play;
  private JMenuItem stop;
  private JMenuItem record;
  private JMenuItem open;
  private JMenuItem save;
  private JMenuItem saveAs;
  private JMenuItem close;

  private MenuBar()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public MenuBar(final QuadCopApp quadCopApp)
  {
    if (quadCopApp == null) {
      throw new NullPointerException("qudCopApp");
    }
    this.quadCopApp = quadCopApp;
    transportControl = quadCopApp.getTransportControl();
    transportControl.addListener(this);
    documentManager = quadCopApp.getDocumentManager();
    documentManager.addListener(this);
    add(createFileMenu());
    add(createEditMenu());
    add(createHelpMenu());
  }

  private JMenu createFileMenu()
  {
    final JMenu file = new JMenu("File");
    file.setMnemonic(KeyEvent.VK_F);

    open = new JMenuItem("Open...");
    open.setMnemonic(KeyEvent.VK_O);
    open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                                               ActionEvent.ALT_MASK));
    open.getAccessibleContext().setAccessibleDescription("Open file");
    open.addActionListener((final ActionEvent event) -> {
        documentManager.open();
      });
    open.setEnabled(true);
    file.add(open);

    save = new JMenuItem("Save");
    save.setMnemonic(KeyEvent.VK_S);
    save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                               ActionEvent.ALT_MASK));
    save.getAccessibleContext().setAccessibleDescription("Save file");
    save.addActionListener((final ActionEvent event) -> {
        documentManager.save();
      });
    save.setEnabled(false);
    file.add(save);

    saveAs = new JMenuItem("Save As...");
    saveAs.getAccessibleContext().setAccessibleDescription("Save file as");
    saveAs.addActionListener((final ActionEvent event) -> {
        documentManager.saveAs();
      });
    saveAs.setEnabled(false);
    file.add(saveAs);

    close = new JMenuItem("Close");
    close.setMnemonic(KeyEvent.VK_C);
    close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                                                ActionEvent.ALT_MASK));
    close.getAccessibleContext().setAccessibleDescription("Close");
    close.addActionListener((final ActionEvent event) -> {
        documentManager.close();
      });
    close.setEnabled(false);
    file.add(close);

    file.addSeparator();

    play = createImageItem("play16x16.png", "Play");
    play.setMnemonic(KeyEvent.VK_P);
    play.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,
                                               ActionEvent.ALT_MASK));
    play.getAccessibleContext().setAccessibleDescription("Play");
    play.addActionListener((final ActionEvent event) -> {
        transportControl.play(documentManager.getDocument());
      });
    play.setEnabled(false);
    file.add(play);

    stop = createImageItem("stop16x16.png", "Stop");
    stop.setMnemonic(KeyEvent.VK_S);
    stop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                                               ActionEvent.ALT_MASK));
    stop.getAccessibleContext().setAccessibleDescription("Stop");
    stop.addActionListener((final ActionEvent event) -> {
        transportControl.stop();
      });
    stop.setEnabled(false);
    file.add(stop);

    record = createImageItem("record16x16.png", "Record");
    record.setMnemonic(KeyEvent.VK_R);
    record.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,
                                               ActionEvent.ALT_MASK));
    record.getAccessibleContext().setAccessibleDescription("Record");
    record.addActionListener((final ActionEvent event) -> {
        final Document document = documentManager.newDocument();
        if (document != null) {
          transportControl.record(document);
        } else {
          // record aborted
        }
      });
    record.setEnabled(true);
    file.add(record);

    file.addSeparator();

    final JMenuItem quit = createImageItem("quit16x16.png", "Quit");
    quit.setMnemonic(KeyEvent.VK_Q);
    quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                               ActionEvent.ALT_MASK));
    quit.getAccessibleContext().setAccessibleDescription("Quit app");
    quit.addActionListener((final ActionEvent event) -> {
        quadCopApp.quit();
      });
    file.add(quit);
    return file;
  }

  private JMenu createEditMenu()
  {
    final JMenu edit = new JMenu("Edit");
    edit.setMnemonic(KeyEvent.VK_E);

    final JMenuItem pianoRoll = new JMenuItem("Piano Roll...");
    pianoRoll.getAccessibleContext().
      setAccessibleDescription("Open Piano Roll Editor");
    pianoRoll.addActionListener((final ActionEvent event) -> {
        JOptionPane.showMessageDialog(quadCopApp,
                                      "Sorry, but the Piano Roll view " +
                                      "of flight control data has " +
                                      "not yet been implemented.",
                                      "Not Yet Implemented",
                                      JOptionPane.ERROR_MESSAGE);
      });
    edit.add(pianoRoll);
    return edit;
  }

  private JMenu createHelpMenu()
  {
    final JMenu help = new JMenu("Help");
    help.setMnemonic(KeyEvent.VK_H);

    final JMenuItem about = new JMenuItem("About...");
    about.setMnemonic(KeyEvent.VK_A);
    about.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,
                                                ActionEvent.ALT_MASK));
    about.getAccessibleContext().setAccessibleDescription("About this app");
    about.addActionListener((final ActionEvent event) -> {
        JOptionPane.showMessageDialog(quadCopApp,
                                      quadCopApp.getDescription(),
                                      "About",
                                      JOptionPane.INFORMATION_MESSAGE);
      });
    help.add(about);
    return help;
  }

  private static JMenuItem createImageItem(final String imageFileName,
                                           final String label)
  {
    final String imagePath = "/images/" + imageFileName;
    final URL imageURL = QuadCopApp.class.getResource(imagePath);
    final JMenuItem item = new JMenuItem(label);
    if (imageURL != null) {
      item.setIcon(new ImageIcon(imageURL, label));
    } else {
      System.err.println("Resource not found: " + imagePath);
    }
    return item;
  }

  public void statusChanged(final TransportControl.Status oldStatus,
                            final TransportControl.Status newStatus)
  {
    switch (newStatus) {
    case STOPPED:
      stop.setEnabled(false);
      if (!documentManager.isDocumentEmpty() && transportControl.canPlay()) {
        play.setEnabled(true);
      }
      if (transportControl.canRecord()) {
        record.setEnabled(true);
      }
      open.setEnabled(true);
      save.setEnabled(documentManager.isModified());
      saveAs.setEnabled(!documentManager.isDocumentEmpty());
      close.setEnabled(!documentManager.isDocumentEmpty());
      break;
    case PLAYING:
      stop.setEnabled(true);
      play.setEnabled(false);
      record.setEnabled(false);
      open.setEnabled(false);
      save.setEnabled(false);
      saveAs.setEnabled(false);
      close.setEnabled(false);
      break;
    case RECORDING:
      stop.setEnabled(true);
      play.setEnabled(false);
      record.setEnabled(false);
      open.setEnabled(false);
      save.setEnabled(false);
      saveAs.setEnabled(false);
      close.setEnabled(false);
      break;
    default:
      throw new IllegalStateException("unknown transport control status");
    }
  }

  public void statusChanged(final DocumentManager.Status status,
                            final int documentSize)
  {
    switch (status) {
    case EMPTY_UNNAMED_DOCUMENT:
      open.setEnabled(true);
      save.setEnabled(false);
      saveAs.setEnabled(false);
      close.setEnabled(false);
      stop.setEnabled(false);
      play.setEnabled(false);
      record.setEnabled(true);
      break;
    case MODIFIED_UNNAMED_DOCUMENT:
      open.setEnabled(true);
      save.setEnabled(true);
      saveAs.setEnabled(true);
      close.setEnabled(true);
      stop.setEnabled(false);
      play.setEnabled(true);
      record.setEnabled(true);
      break;
    case MODIFIED_NAMED_DOCUMENT:
      open.setEnabled(true);
      save.setEnabled(true);
      saveAs.setEnabled(true);
      close.setEnabled(true);
      stop.setEnabled(false);
      play.setEnabled(true);
      record.setEnabled(true);
      break;
    case UNMODIFIED_NAMED_DOCUMENT:
      open.setEnabled(true);
      save.setEnabled(false);
      saveAs.setEnabled(true);
      close.setEnabled(true);
      stop.setEnabled(false);
      play.setEnabled(true);
      record.setEnabled(true);
      break;
    default:
      throw new IllegalStateException("unknown document status");
    }
  }

  public void fileAssociationChanged(final File file)
  {
    // ignore
  }

  public void playProgressChanged(final double progress,
                                  final String progressAsPercent,
                                  final int index,
                                  final int size)
  {
    // ignore
  }

  public void recordProgressChanged(final int size)
  {
    // ignore
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
