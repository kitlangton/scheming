package scheming

object StringUtils {
  def camelToSnakeCase(name: String): String =
    "(?<!^)[A-Z\\d]".r
      .replaceAllIn(name, { m =>
        "_" + m.group(0)
      })
      .toLowerCase
}
