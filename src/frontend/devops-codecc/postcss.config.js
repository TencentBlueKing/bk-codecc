// module.exports = {
//   plugins: [
//     [
//       'postcss-import',
//       {
//         resolve(id, baseDir) {
//           return myResolver.resolveSync({}, baseDir, id);
//         },
//       },
//     ],
//     'postcss-simple-vars',
//     'postcss-mixins',
//     'postcss-nested-ancestors',
//     'postcss-nested',
//     'postcss-preset-env',
//     'postcss-url',
//   ],
// };

module.exports = {
  plugins: {
    'postcss-import': {},
    'postcss-simple-vars': {},
    'postcss-mixins': {},
    'postcss-nested-ancestors': {},
    'postcss-nested': {},
    'tailwindcss/nesting': 'postcss-nesting',
    tailwindcss: {},
    'postcss-preset-env': {},
    'postcss-url': {},
  },
};
