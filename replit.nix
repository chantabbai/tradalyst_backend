{ pkgs }: {
    deps = [
        pkgs.maven
        pkgs.jdk17
        pkgs.jdt-language-server
        pkgs.java-debug
    ];
} 