const fs = require('fs');
const path = require('path');

function walkDir(dir, callback) {
  fs.readdirSync(dir).forEach(f => {
    let dirPath = path.join(dir, f);
    let isDirectory = fs.statSync(dirPath).isDirectory();
    isDirectory ? walkDir(dirPath, callback) : callback(path.join(dir, f));
  });
}

walkDir('C:\\Users\\Minh Khang\\.gemini\\antigravity\\scratch\\codeforge\\src', function(filePath) {
  if (filePath.endsWith('.css')) {
    let content = fs.readFileSync(filePath, 'utf8');
    let original = content;
    
    // Replace white backgrounds
    content = content.replace(/background-color:\s*white;?/gi, 'background-color: var(--cf-bg-surface);');
    content = content.replace(/background:\s*white;?/gi, 'background: var(--cf-bg-surface);');
    content = content.replace(/background-color:\s*#ffffff;?/gi, 'background-color: var(--cf-bg-surface);');
    content = content.replace(/background-color:\s*#fff;?/gi, 'background-color: var(--cf-bg-surface);');
    
    // Replace hardcoded status badge backgrounds to use CSS vars if possible, 
    // or we leave them alone and just let them be light. But wait, in dark mode they look bad if hardcoded.
    // Let's just fix `white` for now.
    
    if (content !== original) {
      fs.writeFileSync(filePath, content, 'utf8');
      console.log('Fixed', filePath);
    }
  }
});
