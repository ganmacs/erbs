package rbparser

object PrettyPrinter {
  def print(ast: ASTs) = {
    println(PrettyPrinter(ast, 0).call)
  }
}

private[rbparser] case class PrettyPrinter(ast: ASTs, private var depth: Int) {
  val buffer = new StringBuilder

  def call: String = {
    _write(ast)
    flush()
  }

  def _write(ast: ASTs) = ast match {
    case literal: Literal[_] => write(literal.toString)
    case Binary(op, lhs, rhs) => write(format(lhs) + s" ${Op.stringfy(op)} " + format(rhs))
    case Assign(lhs, rhs, op) => write(format(lhs) + s" ${Op.stringfy(op)} " + format(rhs))
    case Ary(vars) => write("[" + joinWithComma(vars) + "]")
    case ARef(v, ref) => write(format(v) + "[" + format(ref) + "]")
    case IfExpr(cond, Stmnts(stmnts)) => {
      writeln("if " + format(cond))
      nested { stmnts.foreach { stmnt => indented_write(format(stmnt)+"\n") } }
      indented_write("end")
    }
    case UnlessExpr(cond, Stmnts(stmnts)) => {
      writeln("unless " + format(cond))
      nested { stmnts.foreach { stmnt => indented_write(format(stmnt)+"\n") } }
      indented_write("end")
    }
    case IfModExpr(cond, expr) => write(format(expr) + " if " + format(cond))
    case UnlessModExpr(cond, expr) => write(format(expr) + " unless " + format(cond))
    case Return(args) => {
      val ret = if (args.size == 0) "" else " " + joinWithComma(args)
      write(s"return" + ret)
    }
    case Cmd(rev, name, arg, block) => {
      val recver = rev.fold("")(format(_)+".")
      val args = arg.fold("")(" " + format(_))
      val blck = block.fold("")(format(_))
      write(recver + name + args + blck)
    }
    case Call(rev, name, arg, block) => {
      val recver = rev.fold("")(format(_)+".")
      val args = arg.fold("")("(" + format(_) + ")")
      val blck = block.fold("")(format(_))
      write(recver + name + args + blck)
    }
    case DoBlock(args, Stmnts(stmnts)) => {
      writeln(" do" + args.fold("") (" |" + format(_) + "|"))
      nested { stmnts.foreach { stmnt => indented_write(format(stmnt)+"\n") } }
      indented_write("end")
    }
    case BraceBlock(args, Stmnts(stmnts)) => {
      if (stmnts.size < 2) {
        write(" {" + args.fold("") (" |" + format(_) + "| "))
        stmnts.foreach { stmnt => write(format(stmnt)) }
        write(" }")
      } else {
        writeln(" {" + args.fold("") (" |" + format(_) + "|"))
        nested { stmnts.foreach { stmnt => indented_write(format(stmnt)+"\n") } }
        indented_write("}")
      }
    }
    case Unary(op, expr) => {
      write(Op.stringfy(op))
      write(if (expr.isInstanceOf[Binary]) s"(${format(expr)})" else format(expr))
    }
    case ActualArgs(names) => write(joinWithComma(names))
    case FormalArgs(vars) => vars match {
      case Nil => ()
      case _ => write(joinWithComma(vars))
    }
    case ClassExpr(name, Stmnts(stmnts)) => {
      writeln("class " + format(name))
      nested {
        for (i <- 0 until stmnts.length) {
          val cr = if (i == stmnts.length-1) "\n"  else "\n\n"
          indented_write(format(stmnts(i))+cr)
        }
      }
      indented_write("end")
    }
    case DefExpr(name, args, Stmnts(stmnts)) => {
      writeln("def "+ name + args.fold("") ("(" + format(_) + ")"))
      nested { stmnts.foreach { stmnt => indented_write(format(stmnt)+"\n") } }
      indented_write("end")
    }
    case Stmnts(stmnts) => stmnts.foreach { x => writeln(format(x)) }
  }

  private def flush(): String = buffer.toString()

  private def write(text: String) = buffer.append(text)

  private def writeln(text: String) = write(text+"\n")

  private def indented_write(txt: String) = write(indented+txt)

  private def indented: String = "  " * depth

  private def nested(fn: => Unit) = {
    depth += 1
    fn
    depth -= 1
  }

  private def format(ast: ASTs): String = PrettyPrinter(ast, depth).call

  private def joinWithComma(vars: List[Expr]): String = vars.map(format(_)).mkString(", ")
}
