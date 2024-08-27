import { mavonEditor } from 'mavon-editor';
import 'mavon-editor/dist/css/index.css';

export default {
  name: 'RenderMarkdown',
  functional: true,
  components: {
    mavonEditor,
  },
  props: {
    mds: {
      type: Array,
      required: true,
    },
    copyText: {
      type: Function,
      required: true,
    },
  },
  methods: {
  },
  render(h, ctx) {
    const { mds, copyText } = ctx.props;
    return h(
      'div', {},
      mds.map((md) => {
        if (md.type === 'text') {
          return h(mavonEditor, {
            props: {
              value: md.value,
              defaultOpen: 'preview',
              editable: false,
              subfield: false,
              toolbarsFlag: false,
              boxShadow: false,
              previewBackground: 'rgba(235, 210, 209, 0.4)',
            },
          });
        }
        return h('div', {
          class: 'suggestion-code',
        }, [
          h('span', {
            class: 'codecc-icon icon-copy suggestion-icon-copy cc-link',
            on: {
              click: () => {
                // 复制到剪切板
                copyText(md.code);
              },
            },
          }),
          h(mavonEditor, {
            props: {
              value: md.value,
              defaultOpen: 'preview',
              editable: false,
              subfield: false,
              toolbarsFlag: false,
              boxShadow: false,
              previewBackground: 'rgba(235, 210, 209, 0.4)',
            },
          }),
        ]);
      }),
    );
  },
};
