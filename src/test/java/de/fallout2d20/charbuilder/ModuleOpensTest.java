package de.fallout2d20.charbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.fallout2d20.charbuilder.catalog.origin.OriginData;
import de.fallout2d20.charbuilder.catalog.perk.PerkDefinition;
import de.fallout2d20.charbuilder.catalog.trait.TraitDefinition;
import de.fallout2d20.charbuilder.persistence.CharacterData;
import java.io.IOException;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Tests run on the classpath, where the module system does not restrict reflection. This test
 * catches a missing {@code opens … to com.fasterxml.jackson.databind} before the application fails
 * at runtime: every package of a type Jackson reads – the roots below and every record, enum and
 * type argument reachable from their components – must be opened to Jackson.
 */
class ModuleOpensTest {

  /** Types Jackson deserializes directly: the save format and the catalog entries. */
  private static final Set<Class<?>> JACKSON_ROOTS = Set.of(CharacterData.class, OriginData.class,
      TraitDefinition.class, PerkDefinition.class);

  private static final Pattern OPENS_TO_JACKSON =
      Pattern.compile("opens\\s+([\\w.]+)\\s+to\\s+com\\.fasterxml\\.jackson\\.databind\\s*;");

  @Test
  void everyPackageReadByJacksonIsOpenedToIt() throws IOException {
    Set<String> needed = new TreeSet<>();
    Set<Class<?>> visited = new HashSet<>();
    for (Class<?> root : JACKSON_ROOTS) {
      collect(root, needed, visited);
    }
    assertEquals(needed, new TreeSet<>(needed.stream().filter(openedPackages()::contains).toList()),
        "packages missing an 'opens … to com.fasterxml.jackson.databind' in module-info.java");
  }

  private static Set<String> openedPackages() {
    try {
      String moduleInfo = Files.readString(Path.of("src/main/java/module-info.java"));
      Set<String> opened = new HashSet<>();
      Matcher matcher = OPENS_TO_JACKSON.matcher(moduleInfo);
      while (matcher.find()) {
        opened.add(matcher.group(1));
      }
      return opened;
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void collect(Type type, Set<String> packages, Set<Class<?>> visited) {
    if (type instanceof ParameterizedType parameterized) {
      collect(parameterized.getRawType(), packages, visited);
      for (Type argument : parameterized.getActualTypeArguments()) {
        collect(argument, packages, visited);
      }
    } else if (type instanceof GenericArrayType array) {
      collect(array.getGenericComponentType(), packages, visited);
    } else if (type instanceof Class<?> clazz && visited.add(clazz)) {
      if (clazz.isArray()) {
        collect(clazz.getComponentType(), packages, visited);
        return;
      }
      if (!clazz.getPackageName().startsWith("de.fallout2d20.")) {
        return;
      }
      packages.add(clazz.getPackageName());
      if (clazz.isRecord()) {
        for (RecordComponent component : clazz.getRecordComponents()) {
          collect(component.getGenericType(), packages, visited);
        }
      }
    }
  }
}
