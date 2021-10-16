# Basic Backup

Given a target folder, the program finds all files nested under that folder. It then compares those files to the equivalent in the destination folder. If the destination file doesn't exist or is older (using last modified date), the program copies the file over and overrides the destination. Otherwise it skips copying that file.

`config.txt` is used to tell the program what to backup and to where. The first line is the source and the second line is the destination. As long as there are complete pairs, you can have as many source and destination pairs as you like. You can separate pairs with new lines.

```
./example/in/
./example/out/

./example2/in/
./example2/out/
```

`catalogue.txt` can be used to store all your backup targets