module.exports = function(grunt) {
  grunt.initConfig({
//    pkg: grunt.file.readJSON("package.json"),
    browserify: {
        files: ["app/assets/javascripts/react/*.jsx", ],
        tasks: ["browserify"]
    },
    browserify: {
        options: {
            transform: ["reactify"]
        },
        dist: {
            files: {
              "app/assets/javascripts/react/public/output.js": ["app/assets/javascripts/react/main.jsx"],
              "app/assets/javascripts/react/public/output-nashorn.js": ["app/assets/javascripts/react/nashorn.jsx"]
            }
        }
    }
  });

  grunt.loadNpmTasks("grunt-contrib-watch");
  grunt.loadNpmTasks("grunt-browserify");
};