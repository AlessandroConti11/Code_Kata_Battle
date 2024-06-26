package it.polimi.SE2.CK.utils.folder;

import java.io.IOException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.*;
import java.util.Enumeration;

/**
 * Class that manage the zip files.
 */
public class ZipFolderManager {
    /**
     * Unzip a zip file.
     *
     * @param zipFileName the zip file.
     */
    public static void unzip(String zipFileName){
        try (ZipFile zipFile = new ZipFile(FolderManager.getDirectory() + zipFileName + ".zip" + FolderManager.getPath())) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    File entryFile = new File(FolderManager.getDirectory() + zipFileName + FolderManager.getPath(), entry.getName());
                    if (!entryFile.getParentFile().exists()) {
                        entryFile.getParentFile().mkdirs();
                    }
                    try (
                        InputStream is = zipFile.getInputStream(entry);
                        OutputStream os = new FileOutputStream(entryFile)
                    ){
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete a zip file.
     *
     * @param zipFileName the zip file.
     */
    public static void deleteZipFile(String zipFileName) {
        //zip file path
        File fileToDelete = new File(FolderManager.getDirectory() + zipFileName);

        //check existence of file
        if (fileToDelete.exists() && fileToDelete.isFile()) {
            fileToDelete.delete();
        }
    }
}
