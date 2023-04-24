# Mus
Calculates a checksum and verify it for a directory and its contents (GUI + CLI)

# What is it?

Mus is a little graphical (and CLI!) utility that takes a set of files, or a directory and all its content tree, and calculates a checksum of all the files. It allows to save the checksums in a ```.mu5``` file, and (later) load it and check that all the files are still OK.

I wrote it for checking collections of file for modifications, accidental deletions and corruptions ("bit rot"). 

# How does it work? (GUI)

Ensure to have Java 9 installed, and double-click on the JAR file (or start it in any other way you're accustomed to). The following screen will appear:

<p align="center">
<img src="https://user-images.githubusercontent.com/5470600/35188839-8d88959c-fe3e-11e7-9168-b9872c01166b.JPG" width="700">
</p>

Drag-drop the files or directory to checksum in the window, or press "*Choose Files...*". The checksumming begins, and something like this will be presented:

<p align="center">
<img src="https://user-images.githubusercontent.com/5470600/35188840-8dac2660-fe3e-11e7-8e91-0615ff8f74f2.JPG" width="700">
</p>

Now, you can press "*Save As...*" to decide the location and name of the checsum file (```.mu5```), or press "*Save*" to let the app decide.

After a while ;-), you can just drag-drop (or choose) the ```.mu5``` file to verify it:

<p align="center">
<img src="https://user-images.githubusercontent.com/5470600/35188841-8dd13b94-fe3e-11e7-97e0-5677bb24299c.JPG" width="700">
</p>

<p align="center">
<img src="https://user-images.githubusercontent.com/5470600/35188842-8df2a82e-fe3e-11e7-85a8-a87f1264284d.JPG" width="700">
</p>

See the yellow bit, near the bottom? That's it! Your files are still OK! :-D

# How does it work? (CLI)

From the commandline, it's possible to calculate checksums and to verify them.

To calculate a checksum, either you specify the name/path of the ```.mu5``` file...

```java -jar mus-X.X.X.jar <file1> <file2> <file3> <or_directory> <...> <mu5 file>```

or let Mus to decide it:

```java -jar mus-X.X.X.jar -a <file1> <file2> <file3> <or_directory> <...>```

Either way, it will show a progress indication and do its thing. Example under Windows:

```
C:\Users\GermanoR>java -jar mus-1.1.0.jar -a C:\TEMP\BASE
Mus 1.1.0

Building file tree... Ok.
Checksumming...
Finished.  Speed: --  Time: 0 s
Writing file C:\TEMP\BASE.mu5... Ok.
All done.
```

To verify a checksum, use the ```-v``` switch and specify a ```.mu5``` file or a directory to recurse into.