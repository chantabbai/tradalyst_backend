{ pkgs }: {
    deps = [
        pkgs.maven
        pkgs.jdk17
        pkgs.replitPackages.jdt-language-server
        pkgs.replitPackages.java-debug
    ];
} 