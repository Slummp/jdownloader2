//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jd.parser.Regex;

public class Zip {
    private File destinationFile;
    /**
     * Dateien/Ordner die nicht hinzugefügt werden sollen
     */
    public LinkedList<File> excludeFiles = new LinkedList<File>();
    private Pattern excludeFilter;
    /**
     * Füllt die zipDatei auf die gewünschte größe auf
     */
    public int fillSize = 0;
    private File[] srcFiles;

    /**
     * 
     * @param srcFiles
     *            Datei oder Ordner die dem Ziparchiv hinzugefügt werden sollen.
     * @param destinationFile
     *            die Zieldatei
     */
    public Zip(File srcFile, File destinationFile) {
        this(new File[] { srcFile }, destinationFile);
    }

    /**
     * 
     * @param srcFiles
     *            Dateien oder Ordner die dem Ziparchiv hinzugefügt werden
     *            sollen.
     * @param destinationFile
     *            die Zieldatei
     */
    public Zip(File[] srcFiles, File destinationFile) {
        this.srcFiles = srcFiles;
        this.destinationFile = destinationFile;

    }

    private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {
        if (srcFile.endsWith("Thumbs.db")) { return; }
        if (Regex.matches(srcFile, excludeFilter)) {
            System.out.println("Filtered: " + srcFile);
            return;
        }
        File folder = new File(srcFile);
        if (excludeFiles.contains(folder)) { return; }
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);
        if (excludeFiles.contains(folder)) { return; }
        for (String fileName : folder.list()) {
            if (Regex.matches(fileName, excludeFilter)) {
                System.out.println("Filtered: " + fileName);
                continue;
            }
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
            }
        }
    }

    public void setExcludeFilter(Pattern compile) {
        excludeFilter = compile;

    }

    /**
     * wird aufgerufen um die Zip zu erstellen
     * 
     * @throws Exception
     */
    public void zip() throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destinationFile);
        zip = new ZipOutputStream(fileWriter);
        for (File element : srcFiles) {
            if (element.isDirectory()) {
                addFolderToZip("", element.getAbsolutePath(), zip);
            } else if (element.isFile()) {
                addFileToZip("", element.getAbsolutePath(), zip);
            }
        }

        zip.flush();
        zip.close();
        int toFill = (int) (fillSize - destinationFile.length());

        if (toFill > 0) {
            byte[] sig = new byte[] { 80, 75, 3, 4, 20, 0, 8, 0, 8, 0 };
            toFill -= sig.length;
            FileInputStream in = new FileInputStream(destinationFile);
            File newTarget = new File(destinationFile.getAbsolutePath() + ".jd");
            FileOutputStream out = new FileOutputStream(newTarget);
            out.write(sig);
            out.write(new byte[toFill]);
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
            out.close();
            destinationFile.delete();
            newTarget.renameTo(destinationFile);
        }

    }
}
