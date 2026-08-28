# GhidraEvt

Work-in-progress Paper Mario: The Thousand Year Door and Super Paper Mario evt script disassembly integration for Ghidra.

See the [Issues](https://github.com/SeekyCt/GhidraEvt/issues) tab for an idea of current progress.

## Installing

**Note**: this is a work-in-progress extension, expect bugs.

1. Acquire a build of the extension by either:
    - Building it yourself, which will produce a zip in the `dist` folder
    - Downloading a pre-built copy from [CI](https://github.com/SeekyCt/GhidraEvt/actions)
2. Navigate to your Ghidra installation directory and place the zip in `Extensions/Ghidra/`
    - Do not unzip this file
3. In the main Ghidra window, open `File > Install Extensions` and tick `GhidraEvt`
4. Restart Ghidra
5. Open a project, and you should be prompted to configure plugins
    - If not, the same GUI can be opened through `File > Configure > Miscellaneous`
6. Tick the `GhidraEvt` plugin
7. You should now be able to open the script disassembly view through `Window > Evt Disassembler`
    - Selecting a data address in the listing view will automatically attempt to disassemble it and update the window
    - Options have not yet been implemented, though theming is supported through Ghidra's existing theme system

## Credits

This is created from [Ghidra](https://github.com/NationalSecurityAgency/ghidra/)'s Extension Skeleton and is heavily based on its Decompiler UI code.

