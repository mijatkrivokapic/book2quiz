import { marked } from 'marked';

marked.setOptions({ gfm: true, breaks: false });

export function renderMarkdown(markdown: string): string {
  if (!markdown) {
    return '';
  }
  return marked.parse(markdown, { async: false }) as string;
}

/**
 * Renders Markdown without wrapping block elements (no surrounding <p>), for short,
 * single-line content such as an option or answer. Bound via [innerHTML], which Angular
 * sanitizes.
 */
export function renderMarkdownInline(markdown: string): string {
  if (!markdown) {
    return '';
  }
  return marked.parseInline(markdown, { async: false }) as string;
}
