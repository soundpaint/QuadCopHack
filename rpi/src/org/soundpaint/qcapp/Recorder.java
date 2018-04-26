/*
 * @(#)Recorder.java 1.00 17/01/24
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

public class Recorder
{
  private static class Flags {
    private static final Options.OptionDeclaration optVersion =
      new Options.OptionDeclaration(Options.Type.FLAG, null, false,
                                    new Character('V'), "version",
                                    Options.FlagOptionDefinition.OFF,
                                    "display version information and exit");
    private static final Options.OptionDeclaration optHelp =
      new Options.OptionDeclaration(Options.Type.FLAG, null, false,
                                    new Character('h'), "help",
                                    Options.FlagOptionDefinition.OFF,
                                    "display this help text and exit");
    private static final Options.OptionDeclaration optVerbose =
      new Options.OptionDeclaration(Options.Type.FLAG, null, false,
                                    new Character('v'), "verbose",
                                    Options.FlagOptionDefinition.OFF,
                                    "print verbose information");
    private static final Options.OptionDeclaration optListPorts =
      new Options.OptionDeclaration(Options.Type.FLAG, null, false,
                                    new Character('l'), "list-ports",
                                    Options.FlagOptionDefinition.OFF,
                                    "list available serial ports and exit");
    private static final Options.OptionDeclaration optPort =
      new Options.OptionDeclaration(Options.Type.STRING, "FILE", false,
                                    new Character('p'), "port",
                                    null,
                                    "use FILE as port for serial " +
                                    "communication; default is to use " +
                                    "whatever serial port that is found first");
    private static final Options.OptionDeclaration optOut =
      new Options.OptionDeclaration(Options.Type.STRING, "FILE", false,
                                    new Character('o'), "out",
                                    "quadcop.rec",
                                    "output data to FILE");

    private static final Options.OptionDeclaration[] OPTION_DECLARATIONS =
      new Options.OptionDeclaration[] {
      optVersion, optHelp, optVerbose, optListPorts, optPort, optOut
    };

    private Options.FlagOptionDefinition version;
    private Options.FlagOptionDefinition help;
    private Options.FlagOptionDefinition verbose;
    private Options.FlagOptionDefinition listPorts;
    private Options.StringOptionDefinition port;
    private Options.StringOptionDefinition out;

    private final static Options options;

    static {
      try {
        options = new Options(OPTION_DECLARATIONS);
      } catch (final Options.ParseException ex) {
        throw new RuntimeException("bad option declaration in class Recorder",
                                   ex);
      }
    }

    private Flags()
    {
      throw new RuntimeException("unsupported constructor");
    }

    private Flags(final String argv[]) throws Options.ParseException
    {
      options.parse(argv);
      version = (Options.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optVersion);
      help = (Options.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optHelp);
      verbose = (Options.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optVerbose);
      listPorts = (Options.FlagOptionDefinition)options.
        <Boolean>findDefinitionForDeclaration(optListPorts);
      port = (Options.StringOptionDefinition)options.
        <String>findDefinitionForDeclaration(optPort);
      out = (Options.StringOptionDefinition)options.
        <String>findDefinitionForDeclaration(optOut);
    }

    public boolean checkValidity()
    {
      // currently there are no constrains; flags can be
      // arbitrarily combined
      return true;
    }

    public String getHelp()
    {
      return
        "Usage: Recorder [OPTION]...\n" +
        "Record QuadCop control sketch\n" +
        "\n" +
        options.getHelp();
    }
  }

  private final Flags flags;

  private Recorder()
  {
    throw new RuntimeException("unsupported constructor");
  }

  public Recorder(final String argv[])
    throws Options.ParseException, IOException
  {
    flags = new Flags(argv);
    if (flags.version.isTrue()) {
      printVersion();
    } else if (flags.help.isTrue()) {
      printHelp();
    } else if (flags.listPorts.isTrue()) {
      listPorts();
    } else {
      final QuadCop quadCop = QuadCop.create(System.out, flags.port.getValue());
      record(quadCop);
      quadCop.close();
    }
  }

  private void record(final QuadCop quadCop) throws IOException
  {
    System.out.println("using port " + quadCop.getPortName());
    final String recordFileName = flags.out.getValue();
    final QCFileRecorder recorder = new QCFileRecorder(recordFileName);
    System.out.println("start recording to file " + recordFileName);
    System.out.println("*** Press [Enter] to stop recording. ***");
    quadCop.addRecorder(recorder);
    while (System.in.available() == 0) {
      try {
        Thread.sleep(100);
      } catch (final InterruptedException ex) {
        // ignore
      }
    }
    quadCop.removeRecorder(recorder);
    recorder.close();
    System.out.println("stopped recording");
  }

  private void printVersion()
  {
    System.out.println("Recorder V0.1");
  }

  private void printHelp()
  {
    System.out.println("QuadCop Recorder -- record quad cop control sketch");
    System.out.println();
    System.out.println(flags.getHelp());
  }

  private void listPorts()
  {
    System.out.println("Available serial communication ports:");
    String[] portNames = QuadCop.getAvailablePortNames();
    for (String portName : portNames) {
      System.out.println(portName);
    }
  }

  public static void main(String argv[]) {
    try {
      new Recorder(argv);
    } catch (final Throwable t) {
      System.err.println(t.getMessage());
      System.exit(-1);
    }
  }
}

/*
 * Local Variables:
 *   coding:utf-8
 *   mode:java
 * End:
 */
