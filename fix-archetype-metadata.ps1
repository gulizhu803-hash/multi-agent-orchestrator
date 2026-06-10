param([string]$filePath)

$c = [System.IO.File]::ReadAllText($filePath, [System.Text.Encoding]::UTF8)

# Fix 1: Split png/yml - add filtered="true" to yml fileSet in __rootArtifactId__-app module
$old1 = '        <fileSet encoding="UTF-8">
          <directory>src/main/resources</directory>
          <includes>
            <include>**/*.png</include>
            <include>**/*.yml</include>
          </includes>
        </fileSet>'
$new1 = '        <fileSet encoding="UTF-8">
          <directory>src/main/resources</directory>
          <includes>
            <include>**/*.png</include>
          </includes>
        </fileSet>
        <fileSet filtered="true" encoding="UTF-8">
          <directory>src/main/resources</directory>
          <includes>
            <include>**/*.yml</include>
          </includes>
        </fileSet>'
$c = $c.Replace($old1, $new1)

# Fix 2: Add filtered="true" to build.sh/Dockerfile/push.sh fileSet
$old2 = '        <fileSet encoding="UTF-8">
          <directory></directory>
          <includes>
            <include>build.sh</include>
            <include>Dockerfile</include>
            <include>push.sh</include>
          </includes>
        </fileSet>'
$new2 = '        <fileSet filtered="true" encoding="UTF-8">
          <directory></directory>
          <includes>
            <include>build.sh</include>
            <include>Dockerfile</include>
            <include>push.sh</include>
          </includes>
        </fileSet>'
$c = $c.Replace($old2, $new2)

# Fix 3: Add filtered="true" to docs/dev-ops root fileSet
$old3 = '    <fileSet encoding="UTF-8">
      <directory>docs/dev-ops</directory>
      <includes>
        <include>**/*.sh</include>
        <include>**/*.yml</include>
        <include>**/*.sql</include>
      </includes>
    </fileSet>'
$new3 = '    <fileSet filtered="true" encoding="UTF-8">
      <directory>docs/dev-ops</directory>
      <includes>
        <include>**/*.sh</include>
        <include>**/*.yml</include>
        <include>**/*.sql</include>
      </includes>
    </fileSet>'
$c = $c.Replace($old3, $new3)

[System.IO.File]::WriteAllText($filePath, $c, [System.Text.Encoding]::UTF8)
Write-Host 'archetype-metadata.xml fixed'
