package moe.awawa.taiban.enrich

object RichString {

  val unit: Char = '　'

  implicit class RichString(val self: String) extends AnyRef {

    // TODO 全半角の調整
    def extend(minLength: Int): String = {
      if (self.length < minLength) {
        self + (1 to (minLength - self.length))
          .map(_ => unit)
          .foldLeft("")((l, r) => l + r)
      } else self
    }
  }

}
