package com.mocharealm.accompanist.lyrics.core.utils

internal data class XmlAttribute(
    val name: String,
    val value: String
)

internal data class XmlElement(
    val name: String,
    val attributes: List<XmlAttribute>,
    val children: List<XmlElement>,
    val text: String
)

internal class SimpleXmlParser {
    fun parse(xml: String): XmlElement {
        val stack = ArrayDeque<MutableElement>()
        var i = 0

        while (i < xml.length) {
            when {
                xml[i] == '<' -> {
                    when {
                        xml.startsWith("</", i) -> {
                            val endIndex = xml.indexOf('>', i + 2)
                            if (endIndex == -1) break

                            if (stack.size > 1) {
                                val current = stack.removeLast().toXmlElement()
                                stack.last().children.add(current)
                            }

                            i = endIndex + 1
                        }

                        xml.startsWith("<!--", i) -> {
                            val endIndex = xml.indexOf("-->", i + 4)
                            i = if (endIndex == -1) xml.length else endIndex + 3
                        }

                        xml.startsWith("<?", i) -> {
                            val endIndex = xml.indexOf("?>", i + 2)
                            i = if (endIndex == -1) xml.length else endIndex + 2
                        }

                        else -> {
                            val endIndex = xml.indexOf('>', i + 1)
                            if (endIndex == -1) break

                            var tagPart = xml.substring(i + 1, endIndex)
                            val isSelfClosing = tagPart.endsWith('/')

                            if (isSelfClosing) {
                                tagPart = tagPart
                                    .dropLast(1)
                                    .trim()
                            }

                            val (tagName, attributes) = parseTagAndAttributes(tagPart)
                            val newElement = MutableElement(
                                name = tagName,
                                attributes = attributes.toMutableList()
                            )

                            if (isSelfClosing) {
                                if (stack.isNotEmpty()) {
                                    stack.last().children.add(newElement.toXmlElement())
                                } else {
                                    return newElement.toXmlElement()
                                }
                            } else {
                                stack.addLast(newElement)
                            }

                            i = endIndex + 1
                        }
                    }
                }

                else -> {
                    val nextTagIndex = xml.indexOf('<', i)
                    val rawText = if (nextTagIndex == -1) {
                        xml.substring(i)
                    } else {
                        xml.substring(i, nextTagIndex)
                    }

                    if (rawText.isNotEmpty() && stack.isNotEmpty()) {
                        appendText(stack.last(), rawText)
                    }

                    i = if (nextTagIndex == -1) xml.length else nextTagIndex
                }
            }
        }

        return if (stack.isNotEmpty()) {
            stack.first().toXmlElement()
        } else {
            XmlElement(
                name = "",
                attributes = emptyList(),
                children = emptyList(),
                text = ""
            )
        }
    }

    /**
     * 处理标签之间的文本。
     *
     * - 普通文本：写入当前元素的 text。
     * - 单行空格：保留为 #text，供 TTML 音节间空格使用。
     * - 包含换行的纯空白：通常是 XML 排版缩进，直接忽略；但若换行前
     *   已有内联空格（`</span> \n<span>`），则保留一个空格作为歌词分词。
     */
    private fun appendText(element: MutableElement, rawText: String) {
        if (rawText.isBlank()) {
            val inlinePrefix = rawText.takeWhile { it != '\n' && it != '\r' }
            val hasSemanticInlineSpace = inlinePrefix.any { it == ' ' || it == '\t' }
            val isLayoutWhitespace = rawText.contains('\n') || rawText.contains('\r')

            if (!isLayoutWhitespace || hasSemanticInlineSpace) {
                element.children.add(
                    XmlElement(
                        name = "#text",
                        attributes = emptyList(),
                        children = emptyList(),
                        // XML formatting indentation must not become multiple
                        // visible gaps. A text node here only represents one word
                        // separator, regardless of its indentation width.
                        text = " "
                    )
                )
            }
            return
        }

        element.textBuilder.append(rawText)
    }

    private fun parseTagAndAttributes(tagPart: String): Pair<String, List<XmlAttribute>> {
        val firstSpace = tagPart.indexOfFirst { it.isWhitespace() }

        if (firstSpace == -1) {
            return tagPart to emptyList()
        }

        val tagName = tagPart.substring(0, firstSpace)
        val attributes = mutableListOf<XmlAttribute>()

        var i = firstSpace + 1

        while (i < tagPart.length) {
            while (i < tagPart.length && tagPart[i].isWhitespace()) {
                i++
            }

            if (i >= tagPart.length) break

            val equalsIndex = tagPart.indexOf('=', i)
            if (equalsIndex == -1) break

            val attrName = tagPart.substring(i, equalsIndex).trim()
            i = equalsIndex + 1

            while (i < tagPart.length && tagPart[i].isWhitespace()) {
                i++
            }

            if (i >= tagPart.length) break

            val quote = tagPart[i]

            if (quote == '"' || quote == '\'') {
                val nextQuote = tagPart.indexOf(quote, i + 1)
                if (nextQuote == -1) break

                val attrValue = tagPart.substring(i + 1, nextQuote)
                attributes.add(XmlAttribute(attrName, attrValue))

                i = nextQuote + 1
            } else {
                var nextSpace = i

                while (
                    nextSpace < tagPart.length &&
                    !tagPart[nextSpace].isWhitespace()
                ) {
                    nextSpace++
                }

                val attrValue = tagPart.substring(i, nextSpace)
                attributes.add(XmlAttribute(attrName, attrValue))

                i = nextSpace
            }
        }

        return tagName to attributes
    }

    private class MutableElement(
        val name: String,
        val attributes: MutableList<XmlAttribute> = mutableListOf(),
        val children: MutableList<XmlElement> = mutableListOf(),
        val textBuilder: StringBuilder = StringBuilder()
    ) {
        fun toXmlElement(): XmlElement {
            return XmlElement(
                name = name,
                attributes = attributes,
                children = children,
                text = textBuilder.toString()
            )
        }
    }
}
