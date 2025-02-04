module org.worm {
    exports org.example;
    exports org.worm;

    requires java.sql;
    // requires java.dotenv;

    // SQLite JDBC driver and dotenv are not part of Java SE but need to be on the module path
    requires transitive org.xerial.sqlitejdbc;
}
