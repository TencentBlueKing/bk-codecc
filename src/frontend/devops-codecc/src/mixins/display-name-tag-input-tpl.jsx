export default {
  methods: {
    renderDisplayNameTagInputOption(node) {
      return (
        <div class="display-name-tag-input-options">
          <bk-user-display-name user-id={node.id}></bk-user-display-name>
        </div>
      );
    },
    renderDisplayNameTagInputTag(node) {
      return (
        <div class="display-name-tag-input-tags">
          <bk-user-display-name user-id={node.id}></bk-user-display-name>
        </div>
      );
    },
  },
};
