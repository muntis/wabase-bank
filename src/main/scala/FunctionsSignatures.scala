package uniso.app

trait FunctionSignatures
  extends TresqlFunctions
     with PostgresFunctionSignatures
     with AppCustomDbFunctionSignatures
     with AppMacroSignatures

trait AppCustomDbFunctionSignatures {
  def checked_resolve[T](resolvable: String, resolved: Seq[T], error_message: String): T
  def current_with_substituted(): Any
}

trait AppMacroSignatures {
  def ts_query(field: Object, s: String): Boolean
  def has_role(role: String): Boolean
  def has_role_user(role: String, userId: Long): Boolean
}

trait PostgresFunctionSignatures extends TresqlFunctions {
  def array(arr: Any*): Any
  def concat(str: Any*): String
  def concat_ws(sep: String, str: Any*): String
  def current_date: java.sql.Date // FIXME does not help
  def date(t: java.sql.Timestamp): java.sql.Date
  def date_part(text: String, timestamp: java.sql.Timestamp): java.lang.Double // TODO or interval
  def exists(query: Any): java.lang.Boolean
  def not(b: java.lang.Boolean): java.lang.Boolean
  def now(): java.sql.Timestamp
  def nullif[T](v1: T, v2: Any): T
  def string_agg(expression: String, delimiter: String): String // TODO (T, T): T - text | bytea
  def to_char(t: java.sql.Timestamp, format: String): String
  def to_char(n: java.lang.Long, format: String): String
  def replace(v: String, from: String, to: String): String
}

trait DBAggregateFunctions {
  //aggregate functions
  def count(col: Any): java.lang.Long
  def max[T](col: T): T
  def min[T](col: T): T
  def sum[T](col: T): T
  def avg[T](col: T): T
}

trait TresqlMacroFunctions {
  //macros
  def if_defined[T](variable: Any, exp: T): T
  def if_true[T](variable: Any, exp: T): T
  def if_false[T](variable: Any, exp: T): T
  def if_missing[T](variable: Any, exp: T): T
  def if_any_defined(exp: Any*): Any
  def if_all_defined(exp: Any*): Any
  def if_any_missing(exp: Any*): Any
  def if_all_missing(exp: Any*): Any
  def if_defined_or_else(p0: Any, p1:Any, p2:Any): Any
  def sql_concat(exprs: Any*): Any
  def sql(expr: Any): Any
}

trait BasicDBFunctions {
  def coalesce[T](pars: T*): T
  def upper(string: String): String
  def lower(string: String): String
  def insert (str1: String, offset: Int, length: Int, str2: String): String
  def to_date(date: String, format: String): java.sql.Date
  def trim(string: String): String
  def lpad(string: String, length: Int, lpad_string: String): String
}

trait BasicDialectFunctions {
  //dialect
  def `case`[T](when: Any, `then`: T, rest: Any*): T
  def nextval(seq: String): Long
}

trait TresqlFunctions
  extends DBAggregateFunctions
  with TresqlMacroFunctions
  with BasicDBFunctions
  with BasicDialectFunctions
