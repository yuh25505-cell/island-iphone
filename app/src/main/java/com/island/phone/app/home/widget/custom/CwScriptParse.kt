package com.island.phone.app.home.widget.custom

internal class ScriptError(message: String, val line: Int = 0) : RuntimeException(message, null, false, false)

internal enum class TokType { NUM, STR, TMPL, ID, PUNCT, EOF }

internal class Tok(
    val type: TokType,
    val text: String,
    val line: Int,
    val nl: Boolean,
    val num: Double = 0.0,
    val strs: List<String> = emptyList(),
    val exprs: List<String> = emptyList(),
)

internal object Lexer {
    private val puncts = listOf(
        "...", "===", "!==", "**=", "??=", "||=", "&&=", "<<=", ">>=", "=>", "==", "!=", "<=", ">=",
        "&&", "||", "??", "?.", "++", "--", "+=", "-=", "*=", "/=", "%=", "**", "<<", ">>",
    )
    private const val singles = "{}()[];,.<>+-*/%=!?:&|^~"

    fun lex(src: String): List<Tok> {
        val out = ArrayList<Tok>()
        var i = 0
        var line = 1
        var nl = false
        val n = src.length
        while (i < n) {
            val c = src[i]
            if (c == '\n') {
                line++
                nl = true
                i++
                continue
            }
            if (c.isWhitespace()) {
                i++
                continue
            }
            if (c == '/' && i + 1 < n && src[i + 1] == '/') {
                while (i < n && src[i] != '\n') i++
                continue
            }
            if (c == '/' && i + 1 < n && src[i + 1] == '*') {
                val end = src.indexOf("*/", i + 2)
                if (end < 0) throw ScriptError("注释没有写完，缺少 */", line)
                for (k in i until end) {
                    if (src[k] == '\n') {
                        line++
                        nl = true
                    }
                }
                i = end + 2
                continue
            }
            if (c.isDigit() || (c == '.' && i + 1 < n && src[i + 1].isDigit())) {
                var j = i
                if (c == '0' && i + 1 < n && (src[i + 1] == 'x' || src[i + 1] == 'X')) {
                    j = i + 2
                    while (j < n && (src[j].isDigit() || src[j] in 'a'..'f' || src[j] in 'A'..'F')) j++
                    val v = src.substring(i + 2, j).toLongOrNull(16) ?: throw ScriptError("数字写得不对", line)
                    out.add(Tok(TokType.NUM, src.substring(i, j), line, nl, v.toDouble()))
                } else {
                    while (j < n && (src[j].isDigit() || src[j] == '_')) j++
                    if (j < n && src[j] == '.') {
                        j++
                        while (j < n && src[j].isDigit()) j++
                    }
                    if (j < n && (src[j] == 'e' || src[j] == 'E')) {
                        var k = j + 1
                        if (k < n && (src[k] == '+' || src[k] == '-')) k++
                        if (k < n && src[k].isDigit()) {
                            while (k < n && src[k].isDigit()) k++
                            j = k
                        }
                    }
                    val text = src.substring(i, j).replace("_", "")
                    val v = text.toDoubleOrNull() ?: throw ScriptError("数字「$text」写得不对", line)
                    out.add(Tok(TokType.NUM, text, line, nl, v))
                }
                nl = false
                i = j
                continue
            }
            if (c.isLetter() || c == '_' || c == '$') {
                var j = i + 1
                while (j < n && (src[j].isLetterOrDigit() || src[j] == '_' || src[j] == '$')) j++
                out.add(Tok(TokType.ID, src.substring(i, j), line, nl))
                nl = false
                i = j
                continue
            }
            if (c == '"' || c == '\'') {
                val sb = StringBuilder()
                var j = i + 1
                var closed = false
                while (j < n) {
                    val d = src[j]
                    if (d == c) {
                        closed = true
                        j++
                        break
                    }
                    if (d == '\n') throw ScriptError("字符串没有写完，缺少引号", line)
                    if (d == '\\' && j + 1 < n) {
                        j = readEscape(src, j, sb, line)
                    } else {
                        sb.append(d)
                        j++
                    }
                }
                if (!closed) throw ScriptError("字符串没有写完，缺少引号", line)
                out.add(Tok(TokType.STR, sb.toString(), line, nl))
                nl = false
                i = j
                continue
            }
            if (c == '`') {
                val strs = ArrayList<String>()
                val exprs = ArrayList<String>()
                val sb = StringBuilder()
                var j = i + 1
                var closed = false
                val startLine = line
                while (j < n) {
                    val d = src[j]
                    if (d == '`') {
                        closed = true
                        j++
                        break
                    }
                    if (d == '\\' && j + 1 < n) {
                        j = readEscape(src, j, sb, line)
                    } else if (d == '$' && j + 1 < n && src[j + 1] == '{') {
                        val end = skipBraces(src, j + 2)
                        if (end < 0) throw ScriptError("模板字符串里的 \${ 没有写完", line)
                        strs.add(sb.toString())
                        sb.setLength(0)
                        exprs.add(src.substring(j + 2, end - 1))
                        for (k in j until end) if (src[k] == '\n') line++
                        j = end
                    } else {
                        if (d == '\n') line++
                        sb.append(d)
                        j++
                    }
                }
                if (!closed) throw ScriptError("模板字符串没有写完，缺少 `", startLine)
                strs.add(sb.toString())
                out.add(Tok(TokType.TMPL, "`", startLine, nl, 0.0, strs, exprs))
                nl = false
                i = j
                continue
            }
            var matched: String? = null
            for (p in puncts) {
                if (src.startsWith(p, i)) {
                    matched = p
                    break
                }
            }
            if (matched == "?." && i + 2 < n && src[i + 2].isDigit()) matched = null
            if (matched == null) {
                if (singles.indexOf(c) < 0) throw ScriptError("不认识的符号「$c」", line)
                matched = c.toString()
            }
            out.add(Tok(TokType.PUNCT, matched, line, nl))
            nl = false
            i += matched.length
        }
        out.add(Tok(TokType.EOF, "", line, true))
        return out
    }

    private fun readEscape(src: String, at: Int, sb: StringBuilder, line: Int): Int {
        val e = src[at + 1]
        when (e) {
            'n' -> sb.append('\n')
            't' -> sb.append('\t')
            'r' -> sb.append('\r')
            'b' -> sb.append('\b')
            '0' -> sb.append('\u0000')
            'u' -> {
                if (at + 5 < src.length + 0 && at + 6 <= src.length) {
                    val v = src.substring(at + 2, at + 6).toIntOrNull(16) ?: throw ScriptError("\\u 后面要跟 4 位十六进制", line)
                    sb.append(v.toChar())
                    return at + 6
                }
                throw ScriptError("\\u 后面要跟 4 位十六进制", line)
            }
            'x' -> {
                if (at + 4 <= src.length) {
                    val v = src.substring(at + 2, at + 4).toIntOrNull(16) ?: throw ScriptError("\\x 后面要跟 2 位十六进制", line)
                    sb.append(v.toChar())
                    return at + 4
                }
                throw ScriptError("\\x 后面要跟 2 位十六进制", line)
            }
            '\n' -> {}
            else -> sb.append(e)
        }
        return at + 2
    }

    private fun skipBraces(src: String, start: Int): Int {
        var depth = 1
        var i = start
        while (i < src.length) {
            val c = src[i]
            if (c == '{') {
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0) return i + 1
            } else if (c == '"' || c == '\'') {
                i = skipString(src, i)
                if (i < 0) return -1
                continue
            } else if (c == '`') {
                i = skipTemplate(src, i + 1)
                if (i < 0) return -1
                continue
            }
            i++
        }
        return -1
    }

    private fun skipString(src: String, start: Int): Int {
        val q = src[start]
        var i = start + 1
        while (i < src.length) {
            val c = src[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == q) return i + 1
            i++
        }
        return -1
    }

    private fun skipTemplate(src: String, start: Int): Int {
        var i = start
        while (i < src.length) {
            val c = src[i]
            if (c == '\\') {
                i += 2
                continue
            }
            if (c == '`') return i + 1
            if (c == '$' && i + 1 < src.length && src[i + 1] == '{') {
                i = skipBraces(src, i + 2)
                if (i < 0) return -1
                continue
            }
            i++
        }
        return -1
    }
}

internal sealed class Expr
internal class NumE(val v: Double) : Expr()
internal class StrE(val v: String) : Expr()
internal class TmplE(val strs: List<String>, val exprs: List<Expr>) : Expr()
internal class IdE(val name: String) : Expr()
internal object ThisE : Expr()
internal class LitE(val v: Any?) : Expr()
internal class ArrE(val items: List<Expr>) : Expr()
internal class SpreadE(val e: Expr) : Expr()
internal class ObjProp(val name: String?, val keyExpr: Expr?, val value: Expr?, val spread: Expr?)
internal class ObjE(val props: List<ObjProp>) : Expr()
internal class FuncE(
    val name: String?,
    val params: List<Pat>,
    val body: List<Stmt>,
    val exprBody: Expr?,
    val arrow: Boolean,
    val async: Boolean,
) : Expr()
internal class UnaryE(val op: String, val e: Expr) : Expr()
internal class UpdateE(val op: String, val prefix: Boolean, val target: Expr) : Expr()
internal class BinE(val op: String, val l: Expr, val r: Expr) : Expr()
internal class LogE(val op: String, val l: Expr, val r: Expr) : Expr()
internal class CondE(val c: Expr, val a: Expr, val b: Expr) : Expr()
internal class AssignE(val op: String, val target: Expr, val value: Expr) : Expr()
internal class MemE(val obj: Expr, val prop: Expr, val computed: Boolean, val optional: Boolean) : Expr()
internal class CallE(val callee: Expr, val args: List<Expr>, val optional: Boolean) : Expr()
internal class NewE(val callee: Expr, val args: List<Expr>) : Expr()
internal class AwaitE(val e: Expr) : Expr()
internal class OptE(val e: Expr) : Expr()
internal class SeqE(val list: List<Expr>) : Expr()

internal sealed class Pat
internal class PId(val name: String) : Pat()
internal class PArr(val items: List<Pat?>) : Pat()
internal class PObjEntry(val key: String, val value: Pat)
internal class PObj(val entries: List<PObjEntry>, val rest: Pat?) : Pat()
internal class PDef(val inner: Pat, val def: Expr) : Pat()
internal class PRest(val inner: Pat) : Pat()

internal sealed class Stmt {
    var line: Int = 0
}
internal class ExprS(val e: Expr) : Stmt()
internal class Decl(val pat: Pat, val init: Expr?)
internal class VarS(val kind: String, val decls: List<Decl>) : Stmt()
internal class FuncS(val f: FuncE) : Stmt()
internal class RetS(val e: Expr?) : Stmt()
internal class IfS(val c: Expr, val a: Stmt, val b: Stmt?) : Stmt()
internal class ForS(val init: Stmt?, val test: Expr?, val update: Expr?, val body: Stmt) : Stmt()
internal class ForInS(val kind: String?, val pat: Pat, val iter: Expr, val body: Stmt, val isOf: Boolean) : Stmt()
internal class WhileS(val c: Expr, val body: Stmt) : Stmt()
internal class DoS(val body: Stmt, val c: Expr) : Stmt()
internal class BreakS : Stmt()
internal class ContS : Stmt()
internal class BlockS(val list: List<Stmt>) : Stmt()
internal class TryS(val block: BlockS, val param: Pat?, val handler: BlockS?, val finalizer: BlockS?) : Stmt()
internal class ThrowS(val e: Expr) : Stmt()
internal class Case(val test: Expr?, val body: List<Stmt>)
internal class SwitchS(val disc: Expr, val cases: List<Case>) : Stmt()
internal class EmptyS : Stmt()

internal class Parser(private val toks: List<Tok>) {
    private var p = 0
    private var noIn = false

    companion object {
        private val Reserved = setOf(
            "var", "let", "const", "function", "return", "if", "else", "for", "while", "do", "break", "continue",
            "try", "catch", "finally", "throw", "switch", "case", "default", "new", "delete", "typeof", "void",
            "in", "this", "true", "false", "null", "class", "await",
        )
        private val AssignOps = setOf("=", "+=", "-=", "*=", "/=", "%=", "**=", "??=", "||=", "&&=", "<<=", ">>=")

        fun parseProgram(src: String): List<Stmt> = Parser(Lexer.lex(src)).program()
    }

    fun program(): List<Stmt> {
        val out = ArrayList<Stmt>()
        while (peek().type != TokType.EOF) out.add(statement())
        return out
    }

    private fun peek(): Tok = toks[p]
    private fun at(k: Int): Tok = toks[minOf(p + k, toks.size - 1)]
    private fun next(): Tok {
        val t = toks[p]
        if (p < toks.size - 1) p++
        return t
    }
    private fun isP(s: String): Boolean = toks[p].type == TokType.PUNCT && toks[p].text == s
    private fun isK(s: String): Boolean = toks[p].type == TokType.ID && toks[p].text == s
    private fun eatP(s: String): Boolean {
        if (isP(s)) {
            next()
            return true
        }
        return false
    }
    private fun eatK(s: String): Boolean {
        if (isK(s)) {
            next()
            return true
        }
        return false
    }
    private fun shown(t: Tok): String = if (t.type == TokType.EOF) "代码结尾" else t.text
    private fun fail(msg: String): Nothing = throw ScriptError(msg, peek().line)
    private fun expectP(s: String) {
        if (!eatP(s)) fail("缺少「$s」，这里是「${shown(peek())}」")
    }
    private fun semi() {
        if (eatP(";")) return
        if (isP("}") || peek().type == TokType.EOF || peek().nl) return
        fail("这里缺少分号，或者这一句写得不对：「${shown(peek())}」")
    }

    private fun statement(): Stmt {
        val line = peek().line
        val s = statementInner()
        s.line = line
        return s
    }

    private fun statementInner(): Stmt {
        val t = peek()
        if (t.type == TokType.PUNCT) {
            if (t.text == "{") return block()
            if (t.text == ";") {
                next()
                return EmptyS()
            }
        }
        if (t.type == TokType.ID) {
            when (t.text) {
                "var", "let", "const" -> {
                    val d = varDecl()
                    semi()
                    return d
                }
                "function" -> return funcDecl(false)
                "async" -> {
                    if (at(1).type == TokType.ID && at(1).text == "function") {
                        next()
                        return funcDecl(true)
                    }
                }
                "return" -> {
                    next()
                    var e: Expr? = null
                    if (!isP(";") && !isP("}") && peek().type != TokType.EOF && !peek().nl) e = expression()
                    semi()
                    return RetS(e)
                }
                "if" -> {
                    next()
                    expectP("(")
                    val c = expression()
                    expectP(")")
                    val a = statement()
                    val b = if (eatK("else")) statement() else null
                    return IfS(c, a, b)
                }
                "while" -> {
                    next()
                    expectP("(")
                    val c = expression()
                    expectP(")")
                    return WhileS(c, statement())
                }
                "do" -> {
                    next()
                    val body = statement()
                    if (!eatK("while")) fail("do 后面缺少 while")
                    expectP("(")
                    val c = expression()
                    expectP(")")
                    eatP(";")
                    return DoS(body, c)
                }
                "for" -> return forStatement()
                "break" -> {
                    next()
                    semi()
                    return BreakS()
                }
                "continue" -> {
                    next()
                    semi()
                    return ContS()
                }
                "throw" -> {
                    next()
                    val e = expression()
                    semi()
                    return ThrowS(e)
                }
                "try" -> return tryStatement()
                "switch" -> return switchStatement()
                "class" -> fail("暂不支持 class，请改用函数和对象")
            }
        }
        val e = expression()
        semi()
        return ExprS(e)
    }

    private fun block(): BlockS {
        expectP("{")
        val list = ArrayList<Stmt>()
        while (!isP("}")) {
            if (peek().type == TokType.EOF) fail("缺少「}」")
            list.add(statement())
        }
        expectP("}")
        return BlockS(list)
    }

    private fun varDecl(): VarS {
        val kind = next().text
        val decls = ArrayList<Decl>()
        while (true) {
            val pt = pattern()
            var ini: Expr? = null
            if (eatP("=")) ini = assignment()
            decls.add(Decl(pt, ini))
            if (!eatP(",")) break
        }
        return VarS(kind, decls)
    }

    private fun funcDecl(async: Boolean): Stmt {
        next()
        val nameTok = next()
        if (nameTok.type != TokType.ID) fail("function 后面需要函数名")
        return FuncS(functionRest(nameTok.text, async))
    }

    private fun functionRest(name: String?, async: Boolean): FuncE {
        expectP("(")
        val params = paramList()
        val body = block()
        return FuncE(name, params, body.list, null, false, async)
    }

    private fun paramList(): List<Pat> {
        val params = ArrayList<Pat>()
        while (!isP(")")) {
            if (eatP("...")) {
                params.add(PRest(pattern()))
            } else {
                var pt = pattern()
                if (eatP("=")) pt = PDef(pt, assignment())
                params.add(pt)
            }
            if (!isP(")")) expectP(",")
        }
        expectP(")")
        return params
    }

    private fun pattern(): Pat {
        if (isP("[")) {
            next()
            val items = ArrayList<Pat?>()
            while (!isP("]")) {
                if (isP(",")) {
                    next()
                    items.add(null)
                    continue
                }
                if (eatP("...")) {
                    items.add(PRest(pattern()))
                } else {
                    var pt = pattern()
                    if (eatP("=")) pt = PDef(pt, assignment())
                    items.add(pt)
                }
                if (!isP("]")) expectP(",")
            }
            expectP("]")
            return PArr(items)
        }
        if (isP("{")) {
            next()
            val entries = ArrayList<PObjEntry>()
            var rest: Pat? = null
            while (!isP("}")) {
                if (eatP("...")) {
                    rest = pattern()
                } else {
                    val kt = next()
                    if (kt.type != TokType.ID && kt.type != TokType.STR) fail("解构里的名字写得不对")
                    var value: Pat = PId(kt.text)
                    if (eatP(":")) value = pattern()
                    if (eatP("=")) value = PDef(value, assignment())
                    entries.add(PObjEntry(kt.text, value))
                }
                if (!isP("}")) expectP(",")
            }
            expectP("}")
            return PObj(entries, rest)
        }
        val t = next()
        if (t.type != TokType.ID || t.text in Reserved) fail("这里需要一个名字，不能是「${shown(t)}」")
        return PId(t.text)
    }

    private fun forStatement(): Stmt {
        next()
        expectP("(")
        var init: Stmt? = null
        if (isP(";")) {
            // 没有初始化
        } else if (isK("var") || isK("let") || isK("const")) {
            val kind = next().text
            val first = pattern()
            if (isK("of") || isK("in")) {
                val isOf = next().text == "of"
                val iter = if (isOf) assignment() else expression()
                expectP(")")
                val body = statement()
                return ForInS(kind, first, iter, body, isOf)
            }
            val decls = ArrayList<Decl>()
            var pt = first
            while (true) {
                var ini: Expr? = null
                if (eatP("=")) {
                    noIn = true
                    ini = assignment()
                    noIn = false
                }
                decls.add(Decl(pt, ini))
                if (eatP(",")) pt = pattern() else break
            }
            init = VarS(kind, decls)
        } else {
            if (peek().type == TokType.ID && at(1).type == TokType.ID && (at(1).text == "of" || at(1).text == "in")) {
                val name = next().text
                val isOf = next().text == "of"
                val iter = if (isOf) assignment() else expression()
                expectP(")")
                val body = statement()
                return ForInS(null, PId(name), iter, body, isOf)
            }
            noIn = true
            val e = expression()
            noIn = false
            init = ExprS(e)
        }
        expectP(";")
        val test = if (isP(";")) null else expression()
        expectP(";")
        val update = if (isP(")")) null else expression()
        expectP(")")
        val body = statement()
        return ForS(init, test, update, body)
    }

    private fun tryStatement(): Stmt {
        next()
        val b = block()
        var param: Pat? = null
        var handler: BlockS? = null
        var fin: BlockS? = null
        if (eatK("catch")) {
            if (eatP("(")) {
                param = pattern()
                expectP(")")
            }
            handler = block()
        }
        if (eatK("finally")) fin = block()
        if (handler == null && fin == null) fail("try 后面需要 catch 或 finally")
        return TryS(b, param, handler, fin)
    }

    private fun switchStatement(): Stmt {
        next()
        expectP("(")
        val disc = expression()
        expectP(")")
        expectP("{")
        val cases = ArrayList<Case>()
        while (!isP("}")) {
            var test: Expr? = null
            if (eatK("case")) {
                test = expression()
                expectP(":")
            } else if (eatK("default")) {
                expectP(":")
            } else {
                fail("switch 里面只能写 case 或 default")
            }
            val body = ArrayList<Stmt>()
            while (!isP("}") && !isK("case") && !isK("default")) {
                if (peek().type == TokType.EOF) fail("缺少「}」")
                body.add(statement())
            }
            cases.add(Case(test, body))
        }
        expectP("}")
        return SwitchS(disc, cases)
    }

    fun expression(): Expr {
        val first = assignment()
        if (!isP(",")) return first
        val list = arrayListOf(first)
        while (eatP(",")) list.add(assignment())
        return SeqE(list)
    }

    fun parseExprOnly(): Expr {
        val e = expression()
        if (peek().type != TokType.EOF) fail("这里多了「${shown(peek())}」")
        return e
    }

    private fun arrowStartAt(k: Int): Boolean {
        val t = at(k)
        if (t.type == TokType.ID && t.text !in Reserved) {
            val n = at(k + 1)
            return n.type == TokType.PUNCT && n.text == "=>"
        }
        if (t.type == TokType.PUNCT && t.text == "(") {
            var depth = 0
            var i = k
            while (true) {
                val x = at(i)
                if (x.type == TokType.EOF) return false
                if (x.type == TokType.PUNCT) {
                    if (x.text == "(" || x.text == "[" || x.text == "{") depth++
                    if (x.text == ")" || x.text == "]" || x.text == "}") {
                        depth--
                        if (depth == 0) break
                    }
                }
                i++
            }
            val n = at(i + 1)
            return n.type == TokType.PUNCT && n.text == "=>"
        }
        return false
    }

    private fun assignment(): Expr {
        if (arrowStartAt(0) || (isK("async") && arrowStartAt(1))) return arrow()
        val left = conditional()
        val t = peek()
        if (t.type == TokType.PUNCT && t.text in AssignOps) {
            if (left !is IdE && left !is MemE) fail("等号左边不能赋值")
            next()
            val right = assignment()
            return AssignE(t.text, left, right)
        }
        return left
    }

    private fun arrow(): Expr {
        var async = false
        if (isK("async")) {
            next()
            async = true
        }
        val params = ArrayList<Pat>()
        if (isP("(")) {
            next()
            params.addAll(paramList())
        } else {
            params.add(PId(next().text))
        }
        expectP("=>")
        if (isP("{")) {
            val b = block()
            return FuncE(null, params, b.list, null, true, async)
        }
        val save = noIn
        noIn = false
        val e = assignment()
        noIn = save
        return FuncE(null, params, emptyList(), e, true, async)
    }

    private fun conditional(): Expr {
        val c = binary(1)
        if (isP("?")) {
            next()
            val save = noIn
            noIn = false
            val a = assignment()
            noIn = save
            expectP(":")
            val b = assignment()
            return CondE(c, a, b)
        }
        return c
    }

    private fun binPrec(t: Tok): Int {
        if (t.type == TokType.PUNCT) {
            return when (t.text) {
                "??" -> 1
                "||" -> 2
                "&&" -> 3
                "|" -> 4
                "^" -> 5
                "&" -> 6
                "==", "!=", "===", "!==" -> 7
                "<", ">", "<=", ">=" -> 8
                "<<", ">>" -> 9
                "+", "-" -> 10
                "*", "/", "%" -> 11
                "**" -> 12
                else -> -1
            }
        }
        if (t.type == TokType.ID && (t.text == "in" || t.text == "instanceof")) return 8
        return -1
    }

    private fun binary(minPrec: Int): Expr {
        var left = unary()
        while (true) {
            val t = peek()
            val prec = binPrec(t)
            if (prec < 0 || prec < minPrec) break
            if (t.type == TokType.ID && t.text == "in" && noIn) break
            next()
            val right = if (t.text == "**") binary(prec) else binary(prec + 1)
            left = if (t.text == "&&" || t.text == "||" || t.text == "??") LogE(t.text, left, right) else BinE(t.text, left, right)
        }
        return left
    }

    private fun unary(): Expr {
        val t = peek()
        if (t.type == TokType.PUNCT) {
            if (t.text == "!" || t.text == "-" || t.text == "+" || t.text == "~") {
                next()
                return UnaryE(t.text, unary())
            }
            if (t.text == "++" || t.text == "--") {
                next()
                return UpdateE(t.text, true, unary())
            }
        }
        if (t.type == TokType.ID) {
            if (t.text == "typeof" || t.text == "void" || t.text == "delete") {
                next()
                return UnaryE(t.text, unary())
            }
            if (t.text == "await") {
                next()
                return AwaitE(unary())
            }
        }
        val e = callMember()
        if ((isP("++") || isP("--")) && !peek().nl) {
            val op = next().text
            return UpdateE(op, false, e)
        }
        return e
    }

    private fun args(): List<Expr> {
        val list = ArrayList<Expr>()
        while (!isP(")")) {
            if (eatP("...")) list.add(SpreadE(assignment())) else list.add(assignment())
            if (!isP(")")) expectP(",")
        }
        expectP(")")
        return list
    }

    private fun callMember(): Expr {
        var e: Expr = if (isK("new")) newExpr() else primary()
        var opt = false
        while (true) {
            if (isP(".")) {
                next()
                val n = next()
                if (n.type != TokType.ID) fail("点后面需要一个名字")
                e = MemE(e, StrE(n.text), false, false)
            } else if (isP("?.")) {
                next()
                opt = true
                if (isP("(")) {
                    next()
                    e = CallE(e, args(), true)
                } else if (isP("[")) {
                    next()
                    val k = expression()
                    expectP("]")
                    e = MemE(e, k, true, true)
                } else {
                    val n = next()
                    if (n.type != TokType.ID) fail("?. 后面需要一个名字")
                    e = MemE(e, StrE(n.text), false, true)
                }
            } else if (isP("[")) {
                next()
                val save = noIn
                noIn = false
                val k = expression()
                noIn = save
                expectP("]")
                e = MemE(e, k, true, false)
            } else if (isP("(")) {
                next()
                val save = noIn
                noIn = false
                val a = args()
                noIn = save
                e = CallE(e, a, false)
            } else {
                break
            }
        }
        return if (opt) OptE(e) else e
    }

    private fun newExpr(): Expr {
        next()
        var callee: Expr = if (isK("new")) newExpr() else primary()
        while (true) {
            if (isP(".")) {
                next()
                val n = next()
                if (n.type != TokType.ID) fail("点后面需要一个名字")
                callee = MemE(callee, StrE(n.text), false, false)
            } else if (isP("[")) {
                next()
                val k = expression()
                expectP("]")
                callee = MemE(callee, k, true, false)
            } else {
                break
            }
        }
        val a = if (isP("(")) {
            next()
            args()
        } else {
            emptyList()
        }
        return NewE(callee, a)
    }

    private fun functionExpr(async: Boolean): Expr {
        var name: String? = null
        if (peek().type == TokType.ID) name = next().text
        return functionRest(name, async)
    }

    private fun primary(): Expr {
        val t = next()
        when (t.type) {
            TokType.NUM -> return NumE(t.num)
            TokType.STR -> return StrE(t.text)
            TokType.TMPL -> return TmplE(t.strs, t.exprs.map { Parser(Lexer.lex(it)).parseExprOnly() })
            TokType.ID -> {
                when (t.text) {
                    "true" -> return LitE(true)
                    "false" -> return LitE(false)
                    "null" -> return LitE(null)
                    "this" -> return ThisE
                    "function" -> return functionExpr(false)
                    "async" -> {
                        if (isK("function")) {
                            next()
                            return functionExpr(true)
                        }
                        return IdE("async")
                    }
                    "class" -> fail("暂不支持 class，请改用函数和对象")
                }
                if (t.text in Reserved) throw ScriptError("这里不能用「${t.text}」", t.line)
                return IdE(t.text)
            }
            TokType.PUNCT -> {
                if (t.text == "(") {
                    val save = noIn
                    noIn = false
                    val e = expression()
                    noIn = save
                    expectP(")")
                    return e
                }
                if (t.text == "[") return arrayLiteral()
                if (t.text == "{") return objectLiteral()
                throw ScriptError("这里不应该出现「${t.text}」", t.line)
            }
            TokType.EOF -> throw ScriptError("代码没有写完", t.line)
        }
    }

    private fun arrayLiteral(): Expr {
        val items = ArrayList<Expr>()
        val save = noIn
        noIn = false
        while (!isP("]")) {
            if (isP(",")) {
                next()
                items.add(LitE(Undefined))
                continue
            }
            if (eatP("...")) items.add(SpreadE(assignment())) else items.add(assignment())
            if (!isP("]")) expectP(",")
        }
        expectP("]")
        noIn = save
        return ArrE(items)
    }

    private fun objectLiteral(): Expr {
        val props = ArrayList<ObjProp>()
        val save = noIn
        noIn = false
        while (!isP("}")) {
            if (eatP("...")) {
                props.add(ObjProp(null, null, null, assignment()))
            } else {
                var name: String? = null
                var keyExpr: Expr? = null
                val kt = next()
                if (kt.type == TokType.ID || kt.type == TokType.STR) {
                    name = kt.text
                } else if (kt.type == TokType.NUM) {
                    name = if (kt.num == Math.floor(kt.num)) kt.num.toLong().toString() else kt.num.toString()
                } else if (kt.type == TokType.PUNCT && kt.text == "[") {
                    keyExpr = assignment()
                    expectP("]")
                } else {
                    throw ScriptError("对象里的名字写得不对：「${shown(kt)}」", kt.line)
                }
                val value: Expr
                if (isP("(")) {
                    next()
                    val params = paramList()
                    val body = block()
                    value = FuncE(name, params, body.list, null, false, false)
                } else if (eatP(":")) {
                    value = assignment()
                } else {
                    if (name == null || kt.type != TokType.ID) fail("对象里缺少冒号")
                    value = IdE(name)
                }
                props.add(ObjProp(name, keyExpr, value, null))
            }
            if (!isP("}")) expectP(",")
        }
        expectP("}")
        noIn = save
        return ObjE(props)
    }
}
