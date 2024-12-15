{ pkgs }: {
    deps = [
        pkgs.maven
        pkgs.jdk21
        pkgs.replitPackages.jdt-language-server
        pkgs.replitPackages.java-debug
    ];
} 