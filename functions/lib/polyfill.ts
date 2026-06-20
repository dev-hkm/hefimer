if (typeof globalThis.Node === "undefined") {
  globalThis.Node = class Node {
    static ELEMENT_NODE = 1;
    static ATTRIBUTE_NODE = 2;
    static TEXT_NODE = 3;
    static CDATA_SECTION_NODE = 4;
    static ENTITY_REFERENCE_NODE = 5;
    static ENTITY_NODE = 6;
    static PROCESSING_INSTRUCTION_NODE = 7;
    static COMMENT_NODE = 8;
    static DOCUMENT_NODE = 9;
    static DOCUMENT_TYPE_NODE = 10;
    static DOCUMENT_FRAGMENT_NODE = 11;
    static NOTATION_NODE = 12;

    nodeName: string;
    nodeType: number;
    _textContent: string;
    childNodes: Node[];
    attributes: any[];
    localName: string;
    namespaceURI: string | null;
    documentElement: Node | null = null;

    constructor(nodeName: string, nodeType: number, text = "") {
      this.nodeName = nodeName;
      this.nodeType = nodeType;
      this._textContent = text;
      this.childNodes = [];
      this.attributes = [];
      const colonIndex = nodeName.indexOf(":");
      this.localName = colonIndex > -1 ? nodeName.slice(colonIndex + 1) : nodeName;
      this.namespaceURI = null;
    }

    get textContent(): string {
      if (this.nodeType === 3) return this._textContent;
      return this.childNodes.map(c => c.textContent).join("");
    }

    get firstChild(): Node | null {
      return this.childNodes[0] || null;
    }

    getElementsByTagName(tagName: string): Node[] {
      const results: Node[] = [];
      const traverse = (node: Node) => {
        if (node.nodeName === tagName) {
          results.push(node);
        }
        for (const child of node.childNodes) {
          traverse(child);
        }
      };
      traverse(this);
      return results;
    }

    getAttribute(name: string): string | null {
      return null;
    }
  } as any;
}

// Ensure DOMNode references the global Node
const DOMNodeClass = globalThis.Node as any;

if (typeof globalThis.DOMParser === "undefined") {
  globalThis.DOMParser = class {
    parseFromString(xmlString: string, contentType: string) {
      const regex = /<(\/?[a-zA-Z0-9:_.-]+)([^>]*)>|([^<]+)/g;
      const root = new DOMNodeClass("#document", 9);
      const stack: any[] = [root];
      let match;
      while ((match = regex.exec(xmlString)) !== null) {
        const [_, tag, attrs, text] = match;
        if (tag) {
          if (tag.startsWith("/")) {
            stack.pop();
          } else if (tag.endsWith("/")) {
            const node = new DOMNodeClass(tag.slice(0, -1), 1);
            stack[stack.length - 1].childNodes.push(node);
          } else {
            const node = new DOMNodeClass(tag, 1);
            stack[stack.length - 1].childNodes.push(node);
            stack.push(node);
          }
        } else if (text) {
          const trimmed = text.trim();
          if (trimmed) {
            const node = new DOMNodeClass("#text", 3, trimmed);
            stack[stack.length - 1].childNodes.push(node);
          }
        }
      }
      root.documentElement = root.childNodes.find((n: any) => n.nodeType === 1) || null;
      return root;
    }
  } as any;
}
export {};
