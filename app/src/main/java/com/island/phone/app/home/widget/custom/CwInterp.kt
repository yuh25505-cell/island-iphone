package com.island.phone.app.home.widget.custom

internal class Env(val parent: Env?, val isFunc: Boolean) {
    val vars = HashMap<String, Any?>()
    var thisVal: Any? = Undefined
    var hasThis = false

    fun find(name: String): Env? {
        var e: Env? = this
        while (e != null) {
            if (e.vars.containsKey(name)) return e
            e = e.parent
        }
        return null
    }

    fun funcEnv(): Env {
        var e: Env = this
        while (!e.isFunc) {
            val up = e.parent ?: break
            e = up
        }
        return e
    }
}

internal class BreakSig : RuntimeException(null, null, false, false)
internal class ContSig : RuntimeException(null, null, false, false)
internal class ReturnSig(val value: Any?) : RuntimeException(null, null, false, false)
internal class OptShort : RuntimeException(null, null, false, false)

internal class Closure(val fn: FuncE, val env: Env, private val interp: Interp) : JsFunction() {
    override val fname: String get() = fn.name ?: ""
    override fun call(thisVal: Any?, args: List<Any?>): Any? = interp.callClosure(this, thisVal, args)
}

internal class Interp {
    val global = Env(null, true)
    val logs = ArrayList<String>()
    var line = 0
    private var steps = 0L
    private var depth = 0
    private val maxSteps = 4_000_000L

    init {
        installBuiltins()
    }

    fun resetBudget() {
        steps = 0L
        depth = 0
    }

    private fun tick() {
        steps++
        if (steps > maxSteps) throw BudgetEx()
    }

    fun makeError(type: String, msg: String): JsObject = JsError(type, msg)
    fun typeError(msg: String): JsThrow = JsThrow(JsError("TypeError", msg))
    private fun refError(msg: String): JsThrow = JsThrow(JsError("ReferenceError", msg))

    fun guarded(block: () -> Unit): String? {
        resetBudget()
        return try {
            block()
            null
        } catch (e: ScriptError) {
            if (e.line > 0) "第${e.line}行：${e.message}" else e.message
        } catch (e: JsThrow) {
            "第${line}行：" + errorText(e.value)
        } catch (e: BudgetEx) {
            e.message
        } catch (e: StackOverflowError) {
            "调用层数太深"
        } catch (e: RuntimeException) {
            "脚本出错：" + (e.message ?: e.javaClass.simpleName)
        }
    }

    fun run(src: String) {
        val prog = Parser.parseProgram(src)
        try {
            execBlock(prog, global)
        } catch (r: ReturnSig) {
            // 顶层 return 直接结束
        } catch (b: BreakSig) {
        } catch (c: ContSig) {
        }
    }

    fun callValue(f: Any?, thisVal: Any?, args: List<Any?>): Any? {
        if (f !is JsFunction) return Undefined
        tick()
        return f.call(thisVal, args)
    }

    fun construct(c: JsFunction, args: List<Any?>): Any? {
        if (c is NativeFn) {
            val k = c.ctor ?: throw typeError("${c.name} 不能用 new 来创建")
            return k(args)
        }
        val o = JsObject()
        val r = c.call(o, args)
        return if (r is JsObject) r else o
    }

    private fun flatten(v: Any?): Any? {
        if (v is JsPromise) {
            if (v.rejected) throw JsThrow(v.value)
            return v.value
        }
        return v
    }

    fun callClosure(c: Closure, thisVal: Any?, args: List<Any?>): Any? {
        if (depth >= 150) throw JsThrow(JsError("RangeError", "调用层数太深"))
        depth++
        try {
            tick()
            val f = c.fn
            val env = Env(c.env, true)
            if (!f.arrow) {
                env.thisVal = thisVal
                env.hasThis = true
            }
            for ((i, p) in f.params.withIndex()) {
                if (p is PRest) {
                    bindPattern(p.inner, JsArray(args.drop(i).toMutableList()), env, "let")
                } else {
                    bindPattern(p, args.getOrElse(i) { Undefined }, env, "let")
                }
            }
            if (f.async) {
                return try {
                    JsPromise(flatten(runBody(f, env)))
                } catch (t: JsThrow) {
                    JsPromise(t.value, true)
                }
            }
            return runBody(f, env)
        } finally {
            depth--
        }
    }

    private fun runBody(f: FuncE, env: Env): Any? {
        val ex = f.exprBody
        if (ex != null) return eval(ex, env)
        return try {
            execBlock(f.body, env)
            Undefined
        } catch (r: ReturnSig) {
            r.value
        }
    }

    fun execBlock(list: List<Stmt>, env: Env) {
        for (s in list) {
            if (s is FuncS) {
                val n = s.f.name
                if (n != null) env.vars[n] = Closure(s.f, env, this)
            }
        }
        for (s in list) exec(s, env)
    }

    private fun exec(s: Stmt, env: Env) {
        line = s.line
        when (s) {
            is ExprS -> eval(s.e, env)
            is VarS -> execVar(s, env)
            is FuncS -> {
                val n = s.f.name
                if (n != null && !env.vars.containsKey(n)) env.vars[n] = Closure(s.f, env, this)
            }
            is RetS -> {
                val e = s.e
                throw ReturnSig(if (e == null) Undefined else eval(e, env))
            }
            is IfS -> {
                if (truthy(eval(s.c, env))) {
                    exec(s.a, env)
                } else {
                    val b = s.b
                    if (b != null) exec(b, env)
                }
            }
            is BlockS -> execBlock(s.list, Env(env, false))
            is ForS -> execFor(s, env)
            is ForInS -> execForIn(s, env)
            is WhileS -> {
                while (truthy(eval(s.c, env))) {
                    tick()
                    try {
                        exec(s.body, env)
                    } catch (b: BreakSig) {
                        break
                    } catch (c: ContSig) {
                        // 继续下一轮
                    }
                }
            }
            is DoS -> {
                do {
                    tick()
                    try {
                        exec(s.body, env)
                    } catch (b: BreakSig) {
                        break
                    } catch (c: ContSig) {
                        // 继续下一轮
                    }
                } while (truthy(eval(s.c, env)))
            }
            is BreakS -> throw BreakSig()
            is ContS -> throw ContSig()
            is TryS -> execTry(s, env)
            is ThrowS -> throw JsThrow(eval(s.e, env))
            is SwitchS -> execSwitch(s, env)
            is EmptyS -> {}
        }
    }

    private fun execVar(s: VarS, env: Env) {
        for (d in s.decls) {
            val init = d.init
            if (init == null) {
                val p = d.pat
                if (s.kind == "var" && p is PId && env.funcEnv().vars.containsKey(p.name)) continue
                bindPattern(d.pat, Undefined, env, s.kind)
            } else {
                bindPattern(d.pat, eval(init, env), env, s.kind)
            }
        }
    }

    private fun execFor(s: ForS, env: Env) {
        var cur = Env(env, false)
        val init = s.init
        var names: List<String> = emptyList()
        if (init != null) {
            exec(init, cur)
            if (init is VarS && init.kind != "var") names = cur.vars.keys.toList()
        }
        while (true) {
            val t = s.test
            if (t != null && !truthy(eval(t, cur))) break
            tick()
            try {
                exec(s.body, cur)
            } catch (b: BreakSig) {
                break
            } catch (c: ContSig) {
                // 继续下一轮
            }
            if (names.isNotEmpty()) {
                val nx = Env(env, false)
                for (n in names) nx.vars[n] = cur.vars[n]
                cur = nx
            }
            val u = s.update
            if (u != null) eval(u, cur)
        }
    }

    private fun keysOf(v: Any?): List<Any?> = when (v) {
        is JsArray -> v.list.indices.map { it.toString() }
        is String -> v.indices.map { it.toString() }
        is JsObject -> v.ownKeys()
        else -> emptyList()
    }

    fun iterToList(v: Any?): List<Any?> = when (v) {
        is JsArray -> v.list.toList()
        is String -> v.map { it.toString() }
        else -> throw typeError("这个值不能拿来遍历")
    }

    private fun execForIn(s: ForInS, env: Env) {
        val iterVal = eval(s.iter, env)
        val items: List<Any?> = if (s.isOf) iterToList(iterVal) else keysOf(iterVal)
        for (item in items) {
            tick()
            val e = Env(env, false)
            val kind = s.kind
            if (kind != null) {
                bindPattern(s.pat, item, e, kind)
            } else {
                val pid = s.pat
                if (pid is PId) assignVar(pid.name, item, env)
            }
            try {
                exec(s.body, e)
            } catch (b: BreakSig) {
                break
            } catch (c: ContSig) {
                // 继续下一轮
            }
        }
    }

    private fun execTry(s: TryS, env: Env) {
        try {
            try {
                exec(s.block, env)
            } catch (t: JsThrow) {
                val h = s.handler ?: throw t
                val e = Env(env, false)
                val p = s.param
                if (p != null) bindPattern(p, t.value, e, "let")
                execBlock(h.list, e)
            }
        } finally {
            val f = s.finalizer
            if (f != null) exec(f, env)
        }
    }

    private fun execSwitch(s: SwitchS, env: Env) {
        val d = eval(s.disc, env)
        val e = Env(env, false)
        var matched = -1
        for (i in s.cases.indices) {
            val t = s.cases[i].test ?: continue
            if (strictEq(d, eval(t, e))) {
                matched = i
                break
            }
        }
        if (matched < 0) matched = s.cases.indexOfFirst { it.test == null }
        if (matched < 0) return
        try {
            for (i in matched until s.cases.size) {
                for (st in s.cases[i].body) exec(st, e)
            }
        } catch (b: BreakSig) {
            // switch 结束
        }
    }

    private fun declare(name: String, v: Any?, env: Env, kind: String) {
        if (kind == "var") env.funcEnv().vars[name] = v else env.vars[name] = v
    }

    private fun bindPattern(p: Pat, v: Any?, env: Env, kind: String) {
        when (p) {
            is PId -> declare(p.name, v, env, kind)
            is PDef -> bindPattern(p.inner, if (v === Undefined) eval(p.def, env) else v, env, kind)
            is PRest -> bindPattern(p.inner, v, env, kind)
            is PArr -> {
                val items = iterToList(v)
                for (i in p.items.indices) {
                    val sub = p.items[i] ?: continue
                    if (sub is PRest) {
                        bindPattern(sub.inner, JsArray(items.drop(i).toMutableList()), env, kind)
                    } else {
                        bindPattern(sub, items.getOrElse(i) { Undefined }, env, kind)
                    }
                }
            }
            is PObj -> {
                if (isNullish(v)) throw typeError("不能从 ${toStr(v)} 里取值")
                val used = HashSet<String>()
                for (en in p.entries) {
                    used.add(en.key)
                    bindPattern(en.value, getMember(v, en.key), env, kind)
                }
                val r = p.rest
                if (r != null) {
                    val o = JsObject()
                    if (v is JsObject) {
                        for (k in v.ownKeys()) {
                            if (k !in used) o.props[k] = v.getProp(k)
                        }
                    }
                    bindPattern(r, o, env, kind)
                }
            }
        }
    }

    fun lookup(name: String, env: Env): Any? {
        val e = env.find(name) ?: throw refError("「$name」没有定义")
        return e.vars[name]
    }

    fun assignVar(name: String, v: Any?, env: Env) {
        val e = env.find(name)
        if (e != null) e.vars[name] = v else global.vars[name] = v
    }

    private fun lookupThis(env: Env): Any? {
        var e: Env? = env
        while (e != null) {
            if (e.hasThis) return e.thisVal
            e = e.parent
        }
        return Undefined
    }

    private fun exprName(e: Expr): String = when (e) {
        is IdE -> e.name
        is MemE -> exprName(e.obj) + "." + (if (e.prop is StrE) e.prop.v else "[…]")
        is CallE -> exprName(e.callee) + "()"
        is ThisE -> "this"
        is OptE -> exprName(e.e)
        else -> "这个值"
    }

    private fun memberKey(m: MemE, env: Env): Any? {
        if (m.computed) return eval(m.prop, env)
        val p = m.prop
        return if (p is StrE) p.v else Undefined
    }

    private fun evalArgs(list: List<Expr>, env: Env): List<Any?> {
        if (list.isEmpty()) return emptyList()
        val out = ArrayList<Any?>(list.size)
        for (a in list) {
            if (a is SpreadE) out.addAll(iterToList(eval(a.e, env))) else out.add(eval(a, env))
        }
        return out
    }

    fun eval(e: Expr, env: Env): Any? {
        return when (e) {
            is NumE -> e.v
            is StrE -> e.v
            is LitE -> e.v
            is IdE -> lookup(e.name, env)
            is ThisE -> lookupThis(env)
            is TmplE -> {
                val sb = StringBuilder()
                for (i in e.strs.indices) {
                    sb.append(e.strs[i])
                    if (i < e.exprs.size) sb.append(toStr(eval(e.exprs[i], env)))
                }
                sb.toString()
            }
            is ArrE -> {
                val out = ArrayList<Any?>()
                for (item in e.items) {
                    if (item is SpreadE) out.addAll(iterToList(eval(item.e, env))) else out.add(eval(item, env))
                }
                JsArray(out)
            }
            is SpreadE -> throw typeError("这里不能用 ...")
            is ObjE -> evalObject(e, env)
            is FuncE -> Closure(e, env, this)
            is UnaryE -> evalUnary(e, env)
            is UpdateE -> evalUpdate(e, env)
            is BinE -> {
                val l = eval(e.l, env)
                val r = eval(e.r, env)
                binop(e.op, l, r)
            }
            is LogE -> {
                val l = eval(e.l, env)
                when (e.op) {
                    "&&" -> if (!truthy(l)) l else eval(e.r, env)
                    "||" -> if (truthy(l)) l else eval(e.r, env)
                    else -> if (isNullish(l)) eval(e.r, env) else l
                }
            }
            is CondE -> if (truthy(eval(e.c, env))) eval(e.a, env) else eval(e.b, env)
            is AssignE -> evalAssign(e, env)
            is MemE -> {
                val o = eval(e.obj, env)
                if (e.optional && isNullish(o)) throw OptShort()
                val k = memberKey(e, env)
                if (isNullish(o)) throw typeError("「${exprName(e.obj)}」是 ${toStr(o)}，不能读取「${toStr(k)}」")
                getMember(o, k)
            }
            is CallE -> evalCall(e, env)
            is NewE -> {
                val c = eval(e.callee, env)
                val args = evalArgs(e.args, env)
                if (c !is JsFunction) throw typeError("「${exprName(e.callee)}」不能用 new 来创建")
                construct(c, args)
            }
            is AwaitE -> flatten(eval(e.e, env))
            is OptE -> try {
                eval(e.e, env)
            } catch (s: OptShort) {
                Undefined
            }
            is SeqE -> {
                var last: Any? = Undefined
                for (x in e.list) last = eval(x, env)
                last
            }
        }
    }

    private fun evalObject(e: ObjE, env: Env): Any? {
        val o = JsObject()
        for (pr in e.props) {
            val sp = pr.spread
            if (sp != null) {
                val sv = eval(sp, env)
                if (sv is JsArray) {
                    for (i in sv.list.indices) o.props[i.toString()] = sv.list[i]
                } else if (sv is JsObject) {
                    for (k in sv.ownKeys()) o.props[k] = sv.getProp(k)
                }
            } else {
                val ke = pr.keyExpr
                val k = pr.name ?: if (ke != null) toStr(eval(ke, env)) else ""
                val ve = pr.value
                o.props[k] = if (ve != null) eval(ve, env) else Undefined
            }
        }
        return o
    }

    private fun evalUnary(e: UnaryE, env: Env): Any? {
        val x = e.e
        when (e.op) {
            "typeof" -> {
                if (x is IdE && env.find(x.name) == null) return "undefined"
                return typeOf(eval(x, env))
            }
            "delete" -> {
                if (x is MemE) {
                    val o = eval(x.obj, env)
                    val k = toStr(memberKey(x, env))
                    if (o is JsArray) {
                        val i = k.toIntOrNull()
                        if (i != null && i in o.list.indices) o.list[i] = Undefined
                    } else if (o is JsObject) {
                        o.deleteProp(k)
                    }
                }
                return true
            }
            "void" -> {
                eval(x, env)
                return Undefined
            }
        }
        val v = eval(x, env)
        return when (e.op) {
            "!" -> !truthy(v)
            "-" -> -toNum(v)
            "+" -> toNum(v)
            "~" -> toInt32(toNum(v)).inv().toDouble()
            else -> Undefined
        }
    }

    private fun evalUpdate(e: UpdateE, env: Env): Any? {
        val t = e.target
        if (t is IdE) {
            val old = toNum(lookup(t.name, env))
            val nv = if (e.op == "++") old + 1.0 else old - 1.0
            assignVar(t.name, nv, env)
            return if (e.prefix) nv else old
        }
        if (t is MemE) {
            val o = eval(t.obj, env)
            val k = memberKey(t, env)
            if (isNullish(o)) throw typeError("「${exprName(t.obj)}」是 ${toStr(o)}，不能修改它的属性")
            val old = toNum(getMember(o, k))
            val nv = if (e.op == "++") old + 1.0 else old - 1.0
            setMember(o, k, nv)
            return if (e.prefix) nv else old
        }
        throw typeError("这里不能用 ++ 或 --")
    }

    private fun compound(op: String, cur: Any?, e: AssignE, env: Env): Any? {
        return when (op) {
            "??=" -> if (!isNullish(cur)) cur else eval(e.value, env)
            "||=" -> if (truthy(cur)) cur else eval(e.value, env)
            "&&=" -> if (!truthy(cur)) cur else eval(e.value, env)
            else -> binop(op.dropLast(1), cur, eval(e.value, env))
        }
    }

    private fun evalAssign(e: AssignE, env: Env): Any? {
        val t = e.target
        if (t is IdE) {
            val nv = if (e.op == "=") eval(e.value, env) else compound(e.op, lookup(t.name, env), e, env)
            assignVar(t.name, nv, env)
            return nv
        }
        if (t is MemE) {
            val o = eval(t.obj, env)
            val k = memberKey(t, env)
            if (isNullish(o)) throw typeError("「${exprName(t.obj)}」是 ${toStr(o)}，不能给它设置属性")
            val nv = if (e.op == "=") eval(e.value, env) else compound(e.op, getMember(o, k), e, env)
            setMember(o, k, nv)
            return nv
        }
        throw typeError("等号左边不能赋值")
    }

    private fun evalCall(e: CallE, env: Env): Any? {
        val callee = e.callee
        var thisVal: Any? = Undefined
        val f: Any?
        if (callee is MemE) {
            val o = eval(callee.obj, env)
            if (callee.optional && isNullish(o)) throw OptShort()
            val k = memberKey(callee, env)
            if (isNullish(o)) throw typeError("「${exprName(callee.obj)}」是 ${toStr(o)}，不能调用它的「${toStr(k)}」")
            f = getMember(o, k)
            thisVal = o
        } else {
            f = eval(callee, env)
        }
        if (e.optional && isNullish(f)) throw OptShort()
        val args = evalArgs(e.args, env)
        if (f !is JsFunction) throw typeError("「${exprName(callee)}」不是函数")
        tick()
        return f.call(thisVal, args)
    }

    fun getMember(obj: Any?, key: Any?): Any? {
        when (obj) {
            null, Undefined -> throw typeError("不能读取 ${toStr(obj)} 的「${toStr(key)}」")
            is String -> return stringMember(obj, key)
            is Double -> return numberMember(obj, toStr(key))
            is Boolean -> return Undefined
            is JsArray -> return arrayMember(obj, key)
            is JsFunction -> {
                val k = toStr(key)
                if (obj.props.containsKey(k)) return obj.props[k]
                return functionMember(obj, k)
            }
            is JsDate -> return dateMember(obj, toStr(key))
            is JsPromise -> return promiseMember(obj, toStr(key))
            is JsObject -> {
                val k = if (key is String) key else toStr(key)
                val v = obj.getProp(k)
                if (v !== Undefined || obj.hasProp(k)) return v
                return objectMember(obj, k)
            }
        }
        return Undefined
    }

    private fun setIndex(list: MutableList<Any?>, i: Int, v: Any?) {
        while (list.size <= i) list.add(Undefined)
        list[i] = v
    }

    fun setMember(obj: Any?, key: Any?, v: Any?) {
        when (obj) {
            null, Undefined -> throw typeError("不能给 ${toStr(obj)} 设置「${toStr(key)}」")
            is JsArray -> {
                if (key is Double && key >= 0.0 && key == Math.floor(key) && key < 1e7) {
                    setIndex(obj.list, key.toInt(), v)
                    return
                }
                val k = toStr(key)
                if (k == "length") {
                    val n = toInt32(toNum(v)).coerceAtLeast(0)
                    while (obj.list.size > n) obj.list.removeAt(obj.list.size - 1)
                    while (obj.list.size < n) obj.list.add(Undefined)
                    return
                }
                val idx = k.toIntOrNull()
                if (idx != null && idx >= 0 && idx < 10_000_000) {
                    setIndex(obj.list, idx, v)
                    return
                }
                obj.setProp(k, v)
            }
            is JsObject -> obj.setProp(if (key is String) key else toStr(key), v)
            else -> {}
        }
    }

    private fun less(a: Any?, b: Any?, orEqual: Boolean): Boolean {
        if (a is String && b is String) return if (orEqual) a <= b else a < b
        val x = toNum(a)
        val y = toNum(b)
        if (x.isNaN() || y.isNaN()) return false
        return if (orEqual) x <= y else x < y
    }

    fun add(a: Any?, b: Any?): Any? {
        if (a is Double && b is Double) return a + b
        if (a is String || b is String || a is JsObject || b is JsObject) return toStr(a) + toStr(b)
        return toNum(a) + toNum(b)
    }

    fun binop(op: String, a: Any?, b: Any?): Any? {
        return when (op) {
            "+" -> add(a, b)
            "-" -> toNum(a) - toNum(b)
            "*" -> toNum(a) * toNum(b)
            "/" -> toNum(a) / toNum(b)
            "%" -> {
                val x = toNum(a)
                val y = toNum(b)
                if (y == 0.0 || x.isNaN() || y.isNaN() || x.isInfinite()) Double.NaN else if (y.isInfinite()) x else x % y
            }
            "**" -> Math.pow(toNum(a), toNum(b))
            "==" -> looseEq(a, b)
            "!=" -> !looseEq(a, b)
            "===" -> strictEq(a, b)
            "!==" -> !strictEq(a, b)
            "<" -> less(a, b, false)
            "<=" -> less(a, b, true)
            ">" -> less(b, a, false)
            ">=" -> less(b, a, true)
            "&" -> (toInt32(toNum(a)) and toInt32(toNum(b))).toDouble()
            "|" -> (toInt32(toNum(a)) or toInt32(toNum(b))).toDouble()
            "^" -> (toInt32(toNum(a)) xor toInt32(toNum(b))).toDouble()
            "<<" -> (toInt32(toNum(a)) shl (toInt32(toNum(b)) and 31)).toDouble()
            ">>" -> (toInt32(toNum(a)) shr (toInt32(toNum(b)) and 31)).toDouble()
            "in" -> {
                if (b !is JsObject) throw typeError("in 的右边必须是对象")
                b.hasProp(toStr(a))
            }
            "instanceof" -> instanceOf(a, b)
            else -> throw typeError("不支持的运算「$op」")
        }
    }

    private fun instanceOf(a: Any?, b: Any?): Boolean {
        if (b !is NativeFn) return false
        return when (b.name) {
            "Array" -> a is JsArray
            "Date" -> a is JsDate
            "Object" -> a is JsObject
            "Promise" -> a is JsPromise
            "Function" -> a is JsFunction
            "Error" -> a is JsError
            "TypeError", "RangeError", "SyntaxError", "ReferenceError" -> a is JsError && a.errName == b.name
            else -> false
        }
    }
}
